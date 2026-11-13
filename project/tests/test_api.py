"""
tests/test_api.py
Integration tests for the FastAPI prediction endpoint.
Requires trained model artifacts to be present.

Run: python -m pytest tests/test_api.py -v
"""

import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pytest

# Only run these if model artifacts exist
MODEL_READY = os.path.exists(os.path.join("models", "best_model.pkl"))

if MODEL_READY:
    from fastapi.testclient import TestClient
    from api.main import app
    client = TestClient(app)


@pytest.mark.skipif(not MODEL_READY, reason="Model not trained yet")
class TestAPIEndpoints:

    def test_health_check(self):
        r = client.get("/health")
        assert r.status_code == 200
        data = r.json()
        assert data["status"] == "healthy"
        assert data["model_loaded"] is True

    def test_root(self):
        r = client.get("/")
        assert r.status_code == 200

    def test_predict_high_risk(self):
        payload = {
            "screen_time":  "9+",
            "sleep_hours":  4.0,
            "stress_level": 5,
            "phone_checks": "10+",
            "late_night_usage": "Always",
        }
        r = client.post("/predict", json=payload)
        assert r.status_code == 200
        data = r.json()
        assert "risk_level"     in data
        assert "confidence"     in data
        assert "recommendation" in data
        assert "timestamp"      in data
        assert data["risk_level"] in ["Low", "Moderate", "High", "Severe"]
        assert 0.0 <= data["confidence"] <= 1.0

    def test_predict_low_risk(self):
        payload = {
            "screen_time":  "<2",
            "sleep_hours":  8.5,
            "stress_level": 1,
            "phone_checks": "1-2",
        }
        r = client.post("/predict", json=payload)
        assert r.status_code == 200

    def test_invalid_screen_time(self):
        payload = {
            "screen_time":  "invalid",
            "sleep_hours":  6,
            "stress_level": 2,
            "phone_checks": "3-5",
        }
        r = client.post("/predict", json=payload)
        assert r.status_code == 422

    def test_invalid_stress_level(self):
        payload = {
            "screen_time":  "3-5",
            "sleep_hours":  6,
            "stress_level": 10,   # out of range
            "phone_checks": "3-5",
        }
        r = client.post("/predict", json=payload)
        assert r.status_code == 422

    def test_batch_predict(self):
        payload = {
            "records": [
                {"screen_time": "7-9", "sleep_hours": 5, "stress_level": 4, "phone_checks": "10+"},
                {"screen_time": "<2",  "sleep_hours": 8, "stress_level": 1, "phone_checks": "1-2"},
            ]
        }
        r = client.post("/predict/batch", json=payload)
        assert r.status_code == 200
        data = r.json()
        assert data["total"] == 2
        assert data["processed"] == 2

    def test_risk_levels_info(self):
        r = client.get("/risk-levels")
        assert r.status_code == 200
        data = r.json()
        assert "risk_bands" in data
        assert "Low" in data["risk_bands"]

    def test_recommendations_lookup(self):
        for level in ["Low", "Moderate", "High", "Severe"]:
            r = client.get(f"/recommendations/{level}")
            assert r.status_code == 200

    def test_recommendations_invalid_level(self):
        r = client.get("/recommendations/Unknown")
        assert r.status_code == 404
