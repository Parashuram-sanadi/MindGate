import os
import firebase_admin
from firebase_admin import credentials, auth, messaging
from typing import Optional, List

# Initialize Firebase Admin SDK
_firebase_app = None

def get_firebase_app():
    """Initializes and returns the Firebase app instance."""
    global _firebase_app
    if _firebase_app is not None:
        return _firebase_app

    service_account_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")

    # Fallback to local secrets folder if not set in environment
    if not service_account_path:
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        fallback_path = os.path.join(os.path.dirname(project_root), "secrets", "firebase-service-account.json")
        if os.path.exists(fallback_path):
            service_account_path = fallback_path

    if not service_account_path:
        print("[FIREBASE] ERROR: FIREBASE_SERVICE_ACCOUNT_PATH environment variable not set.")
        return None

    if not os.path.exists(service_account_path):
        print(f"[FIREBASE] ERROR: Service account file not found at {service_account_path}")
        return None

    try:
        cred = credentials.Certificate(service_account_path)
        _firebase_app = firebase_admin.initialize_app(cred)
        print(f"[FIREBASE] Admin SDK initialized for project: {_firebase_app.project_id}")
        return _firebase_app
    except Exception as e:
        print(f"[FIREBASE] ERROR: Failed to initialize Firebase Admin SDK: {type(e).__name__} - {str(e)}")
        return None

def generate_firebase_token(user_id: int, role: str) -> Optional[str]:
    """Generates a Firebase Custom Token for the given user."""
    app = get_firebase_app()
    if not app:
        print("[FIREBASE] Token generation failed: Firebase App not initialized")
        return None

    try:
        # Use existing database user ID as Firebase UID (as string)
        uid = str(user_id).strip()

        # Add user's role as a custom claim
        custom_claims = {
            "role": role
        }

        # Create custom token
        # In firebase-admin 5+, create_custom_token returns bytes in Py3
        token_bytes = auth.create_custom_token(uid, custom_claims, app=app)

        token_str = token_bytes.decode("utf-8") if isinstance(token_bytes, bytes) else token_bytes
        print(f"[FIREBASE] Generated token for UID {uid} (length: {len(token_str)})")
        return token_str
    except Exception as e:
        print(f"[FIREBASE] ERROR: Failed to generate custom token for UID {user_id}: {type(e).__name__} - {str(e)}")
        return None

def send_push_notification(tokens: List[str], title: str, body: str, data: dict = None):
    """Sends a push notification to multiple device tokens."""
    if not tokens:
        return

    app = get_firebase_app()
    if not app:
        return

    try:
        message = messaging.MulticastMessage(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data=data,
            tokens=tokens,
        )
        response = messaging.send_multicast(message)
        print(f"[FIREBASE] PUSH: Successfully sent {response.success_count} messages; failures: {response.failure_count}")
        return response
    except Exception as e:
        print(f"[FIREBASE] ERROR: Failed to send push notification: {e}")
        return None
