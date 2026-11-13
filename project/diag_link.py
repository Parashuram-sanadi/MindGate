import sqlite3
import datetime

db_path = 'D:/MOVIES/gadget_addiction_project/project/mindgate_backend.db'
try:
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    print(f"Server Time: {datetime.datetime.now()}")

    print("\nLatest Users:")
    cur.execute("SELECT id, email, role FROM users ORDER BY id DESC LIMIT 5")
    for r in cur.fetchall(): print(r)

    print("\nLatest Tokens:")
    cur.execute("SELECT id, student_id, created_at, expires_at, used FROM parent_link_token ORDER BY created_at DESC LIMIT 5")
    for r in cur.fetchall(): print(r)

    conn.close()
except Exception as e:
    print(f"Error: {e}")
