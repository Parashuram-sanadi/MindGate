import firebase_admin
from firebase_admin import credentials, firestore
import os
import sys

def main():
    service_account_path = r"D:\MOVIES\gadget_addiction_project\secrets\firebase-service-account.json"

    if not os.path.exists(service_account_path):
        print(f"Error: Service account not found at {service_account_path}")
        return

    try:
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred)
        db = firestore.client()

        print("Searching for PENDING extension requests...")
        # Search for any pending requests
        docs = db.collection("extension_requests").where("status", "==", "PENDING").stream()

        found = False
        for doc in docs:
            found = True
            print(f"Found request: {doc.id}")
            data = doc.to_dict()
            print(f"  Student: {data.get('studentName')} ({data.get('studentUid')})")
            print(f"  App: {data.get('appName')} ({data.get('appPackage')})")

            # Update to APPROVED
            print("  Updating status to APPROVED...")
            doc.reference.update({
                "status": "APPROVED",
                "approvedMinutes": 15,
                "respondedAt": firestore.SERVER_TIMESTAMP
            })
            print("  Done.")

        if not found:
            print("No PENDING requests found in Firestore.")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    main()
