"""All diagrams of Assignment 5 (Graphviz).  Run: python3 make_diagrams.py"""
import subprocess
def render(name, dot):
    open(name + ".dot", "w").write(dot)
    subprocess.run(["dot", "-Tpng", "-Gdpi=140", name + ".dot", "-o", name + ".png"], check=True)
HDR = '#2F5496'
def entity(node, title, rows):
    body = ''.join(f'<tr><td align="left" width="34"><b>{k or "&nbsp;&nbsp;"}</b></td><td align="left">{t}</td></tr>' for k, t in rows)
    return (f'{node} [shape=plain, label=<<table border="0" cellborder="1" cellspacing="0" cellpadding="5"><tr><td colspan="2" bgcolor="{HDR}"><font color="white"><b>{title}</b></font></td></tr>{body}</table>>];\n')
def rel(a, b, label):
    return f'{a} -> {b} [dir=both, arrowtail=teetee, arrowhead=crowodot, label="  {label}  ", fontname="Helvetica", fontsize=11];\n'
ER = 'digraph ER { rankdir=LR; nodesep=0.7; ranksep=1.0; node [fontname="Helvetica", fontsize=11]; edge [fontname="Helvetica"]; labelloc=t; fontname="Helvetica-Bold"; fontsize=14;\n'
def cells(items, colors=None):
    out = ''
    for i, (t, sub) in enumerate(items):
        c = (colors or ['#DDE8F7'] * len(items))[i]
        out += f'<td bgcolor="{c}" cellpadding="6"><b>{t}</b><br/><font point-size="9">{sub}</font></td>'
    return out

# ---------------- A1 layouts ----------------
render("a1_text_layout", f'''digraph G {{ node [fontname="Helvetica", fontsize=11, shape=plain]; labelloc=t; label="A1.1(a) Delimited text file: one pass = one line, 8 fields separated by commas"; fontname="Helvetica-Bold";
 line [label=<<table border="1" cellborder="1" cellspacing="0"><tr>{cells([("passId","P1001"),("regNo","192311001"),("name","Kumar\\\\, Meena"),("route","12"),("stop","Main Gate"),("validity","2027-06-30"),("amount","4500.00"),("crc32","84b0f00e")])}</tr></table>>];
 rule [shape=box, style="rounded,filled", fillcolor="#FFF2CC", label="Delimiter = comma.  Escape rules inside a field:  ,  ->  \\\\,      \\\\  ->  \\\\\\\\      line break  ->  \\\\n\\nSplit only at commas that are NOT escaped.  crc32 = checksum of the first seven fields."];
 ex [shape=box, style="rounded,filled", fillcolor="#E2F0D9", label="Example line (the name is  Kumar, Meena):\\nP1001,192311001,Kumar\\\\, Meena,12,Main Gate,2027-06-30,4500.00,84b0f00e\\n(file is ended by a line break after every record)"];
 line -> rule [style=invis]; rule -> ex [style=invis]; }}''')
render("a1_binary_layout", f'''digraph G {{ node [fontname="Helvetica", fontsize=11, shape=plain]; labelloc=t; label="A1.1(b) Binary file written with DataOutputStream (variable length records)"; fontname="Helvetica-Bold";
 rec [label=<<table border="1" cellborder="1" cellspacing="0"><tr>{cells([("passId","writeUTF: 2-byte length + UTF-8"),("regNo","writeUTF"),("name","writeUTF"),("route","writeInt: 4 bytes"),("stop","writeUTF"),("validity","writeLong: epoch day, 8 bytes"),("amount","writeDouble: 8 bytes")])}</tr></table>>];
 note [shape=box, style="rounded,filled", fillcolor="#FFF2CC", label="No delimiter is needed: every text field starts with its own length, so a name that contains a comma (or any character) is stored unchanged.\\nThe reader must read the fields in EXACTLY this order (readUTF, readUTF, readUTF, readInt, readUTF, readLong, readDouble).\\nA record is about 2+5 + 2+9 + 2+12 + 4 + 2+9 + 8 + 8 = 63 bytes for typical values."];
 rec -> note [style=invis]; }}''')
render("a1_fixed_layout", f'''digraph G {{ node [fontname="Helvetica", fontsize=11, shape=plain]; labelloc=t; label="Alternative: fixed-length records in a RandomAccessFile (92 bytes per record)"; fontname="Helvetica-Bold";
 rec [label=<<table border="1" cellborder="1" cellspacing="0"><tr>{cells([("passId","8 bytes"),("regNo","12"),("name","30"),("route","4 (int)"),("stop","20"),("validity","10"),("amount","8 (double)")])}</tr></table>>];
 note [shape=box, style="rounded,filled", fillcolor="#FFF2CC", label="record i starts at byte  i x 92   ->   seek(i * 92); read 92 bytes      (O(1))\\ntext fields are padded with spaces up to their fixed width: the padding is the wasted space"];
 rec -> note [style=invis]; }}''')

# ---------------- A2 streams ----------------
render("a2_streams", '''digraph G { rankdir=LR; node [shape=box, style="rounded,filled", fontname="Helvetica", fontsize=11]; edge [fontname="Helvetica", fontsize=10];
 labelloc=t; label="A2 - Number of operating-system reads for a 50 MB file"; fontname="Helvetica-Bold";
 subgraph cluster_a { label="Original: FileInputStream.read() one byte at a time"; style=rounded;
   p1 [label="program\\nread() x 52,428,784", fillcolor="#F8CBAD"]; f1 [label="FileInputStream", fillcolor="#DDE8F7"]; os1 [label="operating system\\n52,428,784 reads of 1 byte", fillcolor="#F8CBAD"]; p1 -> f1 -> os1; }
 subgraph cluster_b { label="Redesign: BufferedInputStream (8,192-byte buffer)"; style=rounded;
   p2 [label="program\\nread() x 52,428,784\\n(served from the buffer)", fillcolor="#E2F0D9"]; b2 [label="BufferedInputStream\\nbuffer 8,192 bytes", fillcolor="#FFF2CC"]; f2 [label="FileInputStream", fillcolor="#DDE8F7"]; os2 [label="operating system\\n6,401 reads of 8 KB", fillcolor="#E2F0D9"]; p2 -> b2 -> f2 -> os2; }
 subgraph cluster_c { label="Redesign: own byte[65536] buffer"; style=rounded;
   p3 [label="program\\nread(buf) x 801", fillcolor="#E2F0D9"]; f3 [label="FileInputStream", fillcolor="#DDE8F7"]; os3 [label="operating system\\n801 reads of 64 KB", fillcolor="#E2F0D9"]; p3 -> f3 -> os3; }
}''')

# ---------------- A3 ----------------
render("a3_dao_class", '''digraph G { rankdir=BT; node [shape=record, fontname="Helvetica", fontsize=10, style=filled, fillcolor="#F4F7FB"]; edge [fontname="Helvetica", fontsize=9];
 labelloc=t; label="A3.2 - Generic data-access design"; fontname="Helvetica-Bold";
 dao [label="{\\<\\<interface\\>\\> Dao\\<T, K\\>|+ insert(T)|+ findById(K) : T|+ findAll() : List\\<T\\>|+ update(T) : int|+ delete(K) : int}", fillcolor="#FFF2CC"];
 abs [label="{AbstractJdbcDao\\<T, K\\> (template)|- connections : ConnectionFactory|# query(...), execute(...)  [open, prepare, bind, run, map, close]|# insertSql(), bindInsert(), selectByIdSql(), ...|# mapRow(ResultSet) : T}", fillcolor="#E2F0D9"];
 s [label="{StudentDao|SQL for table student|mapRow -\\> Student}", fillcolor="#DDE8F7"]; c [label="{CourseDao|SQL for table course|mapRow -\\> Course}", fillcolor="#DDE8F7"]; m [label="{MarksDao|SQL for table marks|mapRow -\\> Mark}", fillcolor="#DDE8F7"];
 abs -> dao [arrowhead=empty, style=dashed, label="implements"]; s -> abs [arrowhead=empty, label="extends"]; c -> abs [arrowhead=empty]; m -> abs [arrowhead=empty];
}''')
render("er_a3", ER + ' label="ER diagram - Task A3: student, course and marks";\n'
  + entity("student", "STUDENT", [("PK", "reg_no  VARCHAR(12)   <i>Student.regNo</i>"), ("", "name  VARCHAR(60)   <i>Student.name</i>"), ("", "email  VARCHAR(80)   <i>Student.email</i>"), ("", "year_no  INT   <i>Student.year</i>")])
  + entity("course", "COURSE", [("PK", "course_code  VARCHAR(10)"), ("", "title  VARCHAR(80)"), ("", "credits  INT")])
  + entity("marks", "MARKS", [("PK", "mark_id  INT"), ("FK", "reg_no  VARCHAR(12)"), ("FK", "course_code  VARCHAR(10)"), ("", "marks  INT")])
  + rel("student", "marks", "scores (1 : N)") + rel("course", "marks", "is scored in (1 : N)") + "}")

# ---------------- A4 ----------------
render("a4_object_graph", '''digraph G { rankdir=TB; node [shape=record, fontname="Helvetica", fontsize=10, style=filled, fillcolor="#F4F7FB"]; edge [fontname="Helvetica", fontsize=9];
 labelloc=t; label="A4.1 - Serializing ONE Hostel object writes the whole reachable object graph"; fontname="Helvetica-Bold";
 h [label="{Hostel|name = Block A}", fillcolor="#DDE8F7"]; r1 [label="{Room 101}", fillcolor="#DDE8F7"]; r2 [label="{Room 102}", fillcolor="#DDE8F7"];
 a [label="{Resident Arun|name, regNo : written|password : transient (NOT written)|conn : transient (NOT written)}", fillcolor="#E2F0D9"];
 d [label="{Resident Divya|name, regNo : written|password, conn : transient}", fillcolor="#E2F0D9"];
 conn [label="{java.sql.Connection|NOT Serializable}", fillcolor="#F8CBAD"];
 h -> r1 [label="rooms"]; h -> r2 [label="rooms"]; r1 -> a [label="residents"]; r1 -> d [label="residents"]; r2 -> a [label="same object, written ONCE", color="#C00000", fontcolor="#C00000"];
 a -> conn [style=dashed, color="#C00000", label="skipped (transient)", fontcolor="#C00000"]; d -> conn [style=dashed, color="#C00000"];
}''')

# ---------------- A5 ----------------
render("a5_transaction", '''digraph G { rankdir=LR; node [shape=box, style="rounded,filled", fontname="Helvetica", fontsize=10]; edge [fontname="Helvetica", fontsize=9];
 labelloc=t; label="A5 - Transfer Rs.500: one transaction against two auto-committed statements"; fontname="Helvetica-Bold";
 subgraph cluster_t { label="ONE transaction (setAutoCommit(false))"; style=rounded;
   t0 [label="begin", fillcolor="#DDE8F7"]; t1 [label="UPDATE hostel\\nbalance - 500\\n(not yet permanent)", fillcolor="#FFF2CC"]; tx [label="CRASH / exception", fillcolor="#F8CBAD"]; t2 [label="UPDATE canteen\\nbalance + 500", fillcolor="#FFF2CC"]; tc [label="commit()\\nboth permanent", fillcolor="#E2F0D9"]; tr [label="rollback()\\nneither change kept", fillcolor="#E2F0D9"];
   t0 -> t1 -> t2 -> tc; t1 -> tx [style=dashed, color="#C00000"]; tx -> tr; }
 subgraph cluster_a { label="TWO auto-commit statements"; style=rounded;
   a1 [label="UPDATE hostel\\nbalance - 500\\nCOMMITTED at once", fillcolor="#F8CBAD"]; ax [label="CRASH / exception", fillcolor="#F8CBAD"]; a2 [label="UPDATE canteen\\n(never runs)", fillcolor="#EEEEEE"]; ar [label="Rs.500 is LOST\\n(measured: total 1050 -> 750)", fillcolor="#F8CBAD"];
   a1 -> ax [style=dashed, color="#C00000"]; ax -> a2 [style=dotted]; ax -> ar; }
}''')
render("a5_injection", '''digraph G { rankdir=LR; node [shape=box, style="rounded,filled", fontname="Helvetica", fontsize=10]; edge [fontname="Helvetica", fontsize=9];
 labelloc=t; label="A5.2 - Why string concatenation is unsafe and how PreparedStatement separates code from data"; fontname="Helvetica-Bold";
 subgraph cluster_u { label="UNSAFE: input becomes part of the SQL text"; style=rounded;
   in1 [label="pwd = ' OR '1'='1", fillcolor="#F8CBAD"]; cat [label="\\"... pwd='\\" + pwd + \\"'\\"", fillcolor="#FFF2CC"]; sql1 [label="SELECT * FROM users WHERE id='x' AND pwd='' OR '1'='1'", fillcolor="#F8CBAD"]; db1 [label="database parses the\\nattacker's text as SQL\\n-> every row matches", fillcolor="#F8CBAD"]; in1 -> cat -> sql1 -> db1; }
 subgraph cluster_s { label="SAFE: PreparedStatement"; style=rounded;
   sql2 [label="SELECT * FROM users\\nWHERE id = ? AND pwd = ?\\n(parsed first, fixed)", fillcolor="#E2F0D9"]; in2 [label="setString(2, \\"' OR '1'='1\\")\\nsent as DATA", fillcolor="#DDE8F7"]; db2 [label="compared as an ordinary\\ntext value -> no row", fillcolor="#E2F0D9"]; sql2 -> db2; in2 -> db2; }
}''')
render("er_a5_b2", ER + ' label="ER diagrams - Task A5 (wallet transfer) and Task B2 (login)";\n'
  + entity("account", "ACCOUNT", [("PK", "acc_id  VARCHAR(16)"), ("", "balance  DECIMAL(10,2)  CHECK (balance &gt;= 0)")])
  + entity("users", "USERS (test database only)", [("PK", "user_no  INT"), ("UK", "id  VARCHAR(20)"), ("", "pwd  VARCHAR(40)"), ("", "role  VARCHAR(10)")])
  + "}")

# ---------------- B3 / B4 ----------------
render("b3_loader_flow", '''digraph G { rankdir=TB; node [shape=box, style="rounded,filled", fontname="Helvetica", fontsize=10]; edge [fontname="Helvetica", fontsize=9];
 labelloc=t; label="B3 - PassLoader.loadAll(): one transaction, batches of 1,000"; fontname="Helvetica-Bold";
 s [label="setAutoCommit(false)", fillcolor="#DDE8F7"]; l [label="for each pass:\\nsetString/setInt/setDouble, addBatch()\\nevery 1,000 rows: executeBatch()", fillcolor="#FFF2CC"]; e [label="executeBatch() (the rest)", fillcolor="#FFF2CC"]; c [label="TODO 2: commit()\\nreturn n", fillcolor="#E2F0D9"]; r [label="TODO 3: rollback()\\nre-throw the exception", fillcolor="#F8CBAD"]; f [label="finally: setAutoCommit(old value)", fillcolor="#DDE8F7"];
 s -> l -> e -> c -> f; l -> r [label="NumberFormatException / SQLException", style=dashed, color="#C00000", fontcolor="#C00000"]; e -> r [style=dashed, color="#C00000"]; r -> f;
}''')
render("er_b4", ER + ' label="ER diagram - Task B4: bus passes for 12 campuses";\n'
  + entity("campus", "CAMPUS", [("PK", "campus_id  INT"), ("", "name  VARCHAR(40)  <i>(12 rows)</i>")])
  + entity("pass", "BUS_PASS_FULL", [("PK", "pass_id  VARCHAR(10)"), ("", "reg_no  VARCHAR(12)  <i>index idx_pass_reg</i>"), ("", "name  VARCHAR(60)"), ("", "route_no  INT  <i>index idx_pass_route</i>"), ("", "boarding_stop  VARCHAR(30)"), ("", "validity_date  DATE"), ("", "amount  DECIMAL(8,2)"), ("FK", "campus_id  INT")])
  + entity("issued", "ISSUED (counter log)", [("PK", "pass_no  INT AUTO_INCREMENT  <i>unique number for every counter</i>"), ("", "counter_no  INT"), ("", "issued_at  TIMESTAMP")])
  + rel("campus", "pass", "issues (1 : N)") + "}")
render("b4_architecture", '''digraph G { rankdir=LR; node [shape=box, style="rounded,filled", fontname="Helvetica", fontsize=10]; edge [fontname="Helvetica", fontsize=9];
 labelloc=t; label="B4 - Recommended design: one shared database for 12 campuses and many counters"; fontname="Helvetica-Bold";
 c1 [label="Campus 1\\ncounters 1..n", fillcolor="#DDE8F7"]; c2 [label="Campus 2\\ncounters 1..n", fillcolor="#DDE8F7"]; c12 [label="Campus 12\\ncounters 1..n", fillcolor="#DDE8F7"];
 app [label="pass-issue application\\nPassService (validation)\\nPassDao (PreparedStatement)", fillcolor="#E2F0D9"];
 db [label="MySQL\\nbus_pass_full + indexes\\nAUTO_INCREMENT numbers\\ntransactions, row locks", shape=cylinder, fillcolor="#FFF2CC"];
 mig [label="nightly import\\nMigration: batches + 1 transaction\\n+ failure report", fillcolor="#E2F0D9"]; old [label="existing pass file", fillcolor="#EEEEEE"];
 c1 -> app; c2 -> app; c12 -> app [label="  ..."]; app -> db [label="JDBC"]; old -> mig -> db;
}''')
print("diagrams ok")
