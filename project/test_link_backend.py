import requests
import time

BASE_URL = "http://127.0.0.1:8000"
EMAIL = "test_student_b@gmail.com"
PASSWORD = "password123"

def test():
    # 1. Login
    print(f"Logging in as {EMAIL}...")
    resp = requests.post(
        f"{BASE_URL}/token",
        data={"username": EMAIL, "password": PASSWORD}
    )
    print(f"Login Status: {resp.status_code}")
    if resp.status_code != 200:
        print(resp.text)
        return

    token = resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 2. Call create link code twice
    for i in range(2):
        print(f"\nAttempt {i+1} to create link code...")
        resp = requests.post(f"{BASE_URL}/api/parent/link/create", headers=headers)
        print(f"Status: {resp.status_code}")
        print(f"Body: {resp.text}")
        if resp.status_code != 200:
            break

if __name__ == "__main__":
    test()
