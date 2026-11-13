import sqlite3
import os

db_path = "D:/MOVIES/gadget_addiction_project/project/mindgate_backend.db"

def migrate():
    print(f"Connecting to {db_path}...")
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    try:
        print("Checking if firebase_uid exists...")
        cursor.execute("PRAGMA table_info(users)")
        columns = [row[1] for row in cursor.fetchall()]

        if "firebase_uid" not in columns:
            print("Adding firebase_uid column to users table...")
            cursor.execute("ALTER TABLE users ADD COLUMN firebase_uid TEXT")
            cursor.execute("CREATE UNIQUE INDEX uq_firebase_uid ON users(firebase_uid)")
            print("Column added successfully.")
        else:
            print("firebase_uid column already exists.")

        conn.commit()
    except Exception as e:
        print(f"Error during migration: {e}")
        conn.rollback()
    finally:
        conn.close()

if __name__ == "__main__":
    migrate()
