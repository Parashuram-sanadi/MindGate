import sys, os
import pytest
from fastapi.testclient import TestClient
from datetime import datetime, timezone, timedelta, date

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from api.main import app
from api.database import SessionLocal, Base, engine
from api.models import User, ParentChildRelation, ParentLinkToken
from api.auth import get_password_hash

client = TestClient(app)

@pytest.fixture
def db():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    yield db
    db.close()
    Base.metadata.drop_all(bind=engine)

def get_auth_header(email, password):
    r = client.post("/token", data={"username": email, "password": password})
    token = r.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

class TestParentMonitoring:

    def test_parent_link_workflow(self, db):
        # 1. Create student and parent
        student_data = {"name": "Student", "email": "s@test.com", "password": "pass", "role": "student"}
        parent_data = {"name": "Parent", "email": "p@test.com", "password": "pass", "role": "parent"}
        client.post("/api/register", json=student_data)
        client.post("/api/register", json=parent_data)

        s_auth = get_auth_header("s@test.com", "pass")
        p_auth = get_auth_header("p@test.com", "pass")

        # 2. Student creates link code
        r = client.post("/api/parent/link/create", headers=s_auth)
        assert r.status_code == 200
        code = r.json()["code"]

        # 3. Parent accepts link code
        r = client.post("/api/parent/link/accept", json={"code": code}, headers=p_auth)
        assert r.status_code == 200
        assert r.json()["student_name"] == "Student"

        # 4. Verify link in child list
        r = client.get("/api/parent/children", headers=p_auth)
        assert len(r.json()) == 1
        assert r.json()[0]["name"] == "Student"

    def test_usage_limit_event_and_dedup(self, db):
        # Setup
        client.post("/api/register", json={"name": "S", "email": "s@t.com", "password": "p", "role": "student"})
        client.post("/api/register", json={"name": "P", "email": "p@t.com", "password": "p", "role": "parent"})
        s_auth = get_auth_header("s@t.com", "p")
        p_auth = get_auth_header("p@t.com", "p")

        # Link them
        code = client.post("/api/parent/link/create", headers=s_auth).json()["code"]
        client.post("/api/parent/link/accept", json={"code": code}, headers=p_auth)

        # Report event - exactly at a bucket start
        ts = datetime(2026, 6, 27, 10, 0, 0, tzinfo=timezone.utc)
        event_data = {
            "app_package": "com.social",
            "limit_seconds": 3600,
            "actual_usage_seconds": 3700,
            "timestamp": ts.isoformat().replace("+00:00", "Z"),
            "event_date": "2026-06-27"
        }
        r = client.post("/api/usage/limit-event", json=event_data, headers=s_auth)
        assert r.status_code == 202
        assert r.json()["notification"] == "sent"

        # Report second event within 20 mins (e.g. 10 mins later) -> same bucket
        ts_soon = ts + timedelta(minutes=10)
        event_data["timestamp"] = ts_soon.isoformat().replace("+00:00", "Z")
        r = client.post("/api/usage/limit-event", json=event_data, headers=s_auth)
        assert r.status_code == 202
        assert r.json()["notification"] == "suppressed_duplicate"

        # Report third event after 20 mins (e.g. 25 mins later) -> next bucket
        ts_later = ts + timedelta(minutes=25)
        event_data["timestamp"] = ts_later.isoformat().replace("+00:00", "Z")
        r = client.post("/api/usage/limit-event", json=event_data, headers=s_auth)
        assert r.status_code == 202
        assert r.json()["notification"] == "sent"

    def test_authorization_protection(self, db):
        # Setup two parents
        client.post("/api/register", json={"name": "S", "email": "s@t.com", "password": "p", "role": "student"})
        client.post("/api/register", json={"name": "P1", "email": "p1@t.com", "password": "p", "role": "parent"})
        client.post("/api/register", json={"name": "P2", "email": "p2@t.com", "password": "p", "role": "parent"})

        s_auth = get_auth_header("s@t.com", "p")
        p1_auth = get_auth_header("p1@t.com", "p")
        p2_auth = get_auth_header("p2@t.com", "p")

        # Link S to P1
        code = client.post("/api/parent/link/create", headers=s_auth).json()["code"]
        r = client.post("/api/parent/link/accept", json={"code": code}, headers=p1_auth)
        s_id = r.json()["student_id"]

        # P1 can see status
        r = client.get(f"/api/parent/children/{s_id}/status", headers=p1_auth)
        assert r.status_code == 200

        # P2 cannot see status
        r = client.get(f"/api/parent/children/{s_id}/status", headers=p2_auth)
        assert r.status_code == 403
