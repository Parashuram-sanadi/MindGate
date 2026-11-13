import sqlite3
import os

db_path = 'mindgate_backend.db'
if os.path.exists(db_path):
    conn = sqlite3.connect(db_path)
    try:
        conn.execute('ALTER TABLE users ADD COLUMN role_selected BOOLEAN NOT NULL DEFAULT 0')
        conn.commit()
        print("Successfully added role_selected column")
    except sqlite3.OperationalError as e:
        if "duplicate column name" in str(e):
            print("Column already exists")
        else:
            print(f"Error: {e}")
    finally:
        conn.close()

# Also ensure active_limit_session table exists
# This should be handled by SQLAlchemy's create_all, but we'll see.
