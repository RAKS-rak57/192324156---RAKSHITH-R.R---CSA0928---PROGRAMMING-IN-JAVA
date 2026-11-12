"""
Checks the LOGIC of campus_events.sql (constraints, joins, paging) in SQLite, because no MySQL server was available.
The MySQL script is converted mechanically (AUTO_INCREMENT, ENGINE, INDEX, CREATE DATABASE/USE). SQLite is NOT MySQL:
error texts and some syntax differ, so this proves the table design and the queries, not the MySQL driver or server.
Run:  python3 sql_logic_check.py
"""
import re, sqlite3, time

script = open("campus_events.sql").read()
script = re.sub(r"CREATE DATABASE[^;]*;", "", script)
script = re.sub(r"USE [^;]*;", "", script)
script = script.replace("INT AUTO_INCREMENT PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT")
script = re.sub(r"\)\s*ENGINE=InnoDB;", ");", script)
script = re.sub(r",\s*INDEX idx_reg_event \(event_id\)[^\n]*", "", script)

db = sqlite3.connect(":memory:")
db.execute("PRAGMA foreign_keys = ON")
db.executescript(script)
db.execute("CREATE INDEX idx_reg_event ON registration(event_id)")       # the INDEX line of the MySQL script

print("TABLES        :", [r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")])
print("seed rows     : students=%d events=%d registrations=%d" % tuple(db.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0] for t in ("student", "event", "registration")))

print("\n-- RULE 1: one registration per student per event (UNIQUE (reg_no, event_id))")
try:
    db.execute("INSERT INTO registration(reg_no, event_id) VALUES ('192311001', 1)")
    print("   duplicate accepted  <-- WRONG")
except sqlite3.IntegrityError as e:
    print("   duplicate rejected  :", e)
db.execute("INSERT INTO registration(reg_no, event_id) VALUES ('192311001', 2)")
print("   same student, other event accepted: OK")

print("\n-- RULE 2: registration must point to an existing student and event (FOREIGN KEY)")
for reg, ev in (("999999999", 1), ("192311001", 99)):
    try:
        db.execute("INSERT INTO registration(reg_no, event_id) VALUES (?, ?)", (reg, ev))
        print("   accepted <-- WRONG", reg, ev)
    except sqlite3.IntegrityError as e:
        print("   (%s, %d) rejected  :" % (reg, ev), e)

print("\n-- Upsert of the student row (MySQL: ON DUPLICATE KEY UPDATE; SQLite: ON CONFLICT DO UPDATE)")
db.execute("INSERT INTO student(reg_no,name,email) VALUES ('192311001','Arun K','arun.new@example.com') "
           "ON CONFLICT(reg_no) DO UPDATE SET name=excluded.name, email=excluded.email")
print("  ", db.execute("SELECT * FROM student WHERE reg_no='192311001'").fetchone())

PAGE_SQL = ("SELECT r.reg_id, r.reg_no, s.name, e.title, r.registered_on FROM registration r "
            "JOIN student s ON s.reg_no = r.reg_no JOIN event e ON e.event_id = r.event_id "
            "{where} ORDER BY r.reg_id LIMIT ? OFFSET ?")
print("\n-- The paging query used by JdbcRegistrationDao.page()  (LIMIT 2 OFFSET 0, then OFFSET 2)")
for off in (0, 2):
    for row in db.execute(PAGE_SQL.format(where=""), (2, off)): print("  ", row[:4])
    print("   ---")
print("-- Filter by event 1")
for row in db.execute(PAGE_SQL.format(where="WHERE r.event_id = ?"), (1, 10, 0)): print("  ", row[:4])
print("   query plan:", [r[3] for r in db.execute("EXPLAIN QUERY PLAN " + PAGE_SQL.format(where="WHERE r.event_id = ?"), (1, 10, 0))][:3])

print("\n-- Cost of paging with 100,000 registrations (SQLite stand-in, one run each, milliseconds)")
db.execute("PRAGMA foreign_keys = OFF")
db.executemany("INSERT INTO student(reg_no,name,email) VALUES (?,?,?)", [("%09d" % (100000000 + i), "Student %d" % i, "s%d@x.com" % i) for i in range(100000)])
db.executemany("INSERT INTO registration(reg_no,event_id) VALUES (?,1)", [("%09d" % (100000000 + i),) for i in range(100000)])
def timed(label, sql, params):
    db.execute(sql, params).fetchall()                      # warm
    t = time.perf_counter(); rows = db.execute(sql, params).fetchall(); ms = (time.perf_counter() - t) * 1000
    print("   %-52s %4d rows  %8.2f ms" % (label, len(rows), ms))
timed("all rows (no LIMIT)", PAGE_SQL.format(where="").replace("LIMIT ? OFFSET ?", "LIMIT -1 OFFSET 0"), ())
timed("page of 50, OFFSET 0", PAGE_SQL.format(where=""), (50, 0))
timed("page of 50, OFFSET 50,000", PAGE_SQL.format(where=""), (50, 50000))
timed("page of 50, OFFSET 99,950 (last page)", PAGE_SQL.format(where=""), (50, 99950))
