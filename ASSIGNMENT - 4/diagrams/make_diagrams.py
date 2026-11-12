"""Generates all diagrams (ER diagrams, class / container / flow diagrams) with Graphviz.  Run: python3 make_diagrams.py"""
import subprocess

def render(name, dot):
    open(name + ".dot", "w").write(dot)
    subprocess.run(["dot", "-Tpng", "-Gdpi=140", name + ".dot", "-o", name + ".png"], check=True)

HDR = '#2F5496'
def entity(node, title, rows):
    """rows: list of (key, text) key in PK / FK / UK / ''"""
    body = ''.join(f'<tr><td align="left" width="34"><b>{k or '&nbsp;&nbsp;'}</b></td><td align="left">{t}</td></tr>' for k, t in rows)
    return (f'{node} [shape=plain, label=<<table border="0" cellborder="1" cellspacing="0" cellpadding="5">'
            f'<tr><td colspan="2" bgcolor="{HDR}"><font color="white"><b>{title}</b></font></td></tr>{body}</table>>];\n')
def rel(a, b, label):
    return f'{a} -> {b} [dir=both, arrowtail=teetee, arrowhead=crowodot, label="  {label}  ", fontname="Helvetica", fontsize=11];\n'
ER_HEAD = 'digraph ER { rankdir=LR; nodesep=0.7; ranksep=1.0; node [fontname="Helvetica", fontsize=11]; edge [fontname="Helvetica"];\n labelloc=t; fontname="Helvetica-Bold"; fontsize=14;\n'

# ---------------- ER diagrams ----------------
render("er_task1", ER_HEAD + ' label="ER diagram - Task 1: Student fee payment";\n'
  + entity("student", "STUDENT", [("PK", "reg_no  VARCHAR(12)"), ("", "name  VARCHAR(60)")])
  + entity("fees", "FEES", [("PK", "payment_id  INT AUTO_INCREMENT"), ("FK", "reg_no  VARCHAR(12)"), ("", "amount  DECIMAL(10,2)"), ("", "paid_on  TIMESTAMP")])
  + rel("student", "fees", "pays (1 : N)") + "}")

render("er_task2", ER_HEAD + ' label="ER diagram - Task 2: Outpatient appointment desk";\n'
  + entity("department", "DEPARTMENT", [("PK", "dept_id  INT"), ("UK", "name  VARCHAR(40)")])
  + entity("doctor", "DOCTOR", [("PK", "doctor_id  VARCHAR(8)"), ("", "name  VARCHAR(60)"), ("FK", "dept_id  INT")])
  + entity("patient", "PATIENT", [("PK", "patient_id  VARCHAR(8)"), ("", "name  VARCHAR(60)"), ("", "phone  VARCHAR(15)")])
  + entity("appointment", "APPOINTMENT", [("PK", "appt_id  INT AUTO_INCREMENT"), ("FK", "patient_id  VARCHAR(8)"), ("FK", "doctor_id  VARCHAR(8)"),
        ("", "appt_date  DATE"), ("", "slot_time  CHAR(5)"), ("UK", "(doctor_id, appt_date, slot_time)")])
  + rel("department", "doctor", "has (1 : N)") + rel("patient", "appointment", "books (1 : N)") + rel("doctor", "appointment", "is booked in (1 : N)") + "}")

render("er_task6", ER_HEAD + ' label="ER diagram - Task 6: Campus event registration (M : N resolved by REGISTRATION)";\n'
  + entity("student", "STUDENT", [("PK", "reg_no  VARCHAR(12)"), ("", "name  VARCHAR(60)"), ("", "email  VARCHAR(80)")])
  + entity("registration", "REGISTRATION", [("PK", "reg_id  INT AUTO_INCREMENT"), ("FK", "reg_no  VARCHAR(12)"), ("FK", "event_id  INT"),
        ("", "registered_on  TIMESTAMP"), ("UK", "(reg_no, event_id)  <i>one per student per event</i>")])
  + entity("event", "EVENT", [("PK", "event_id  INT AUTO_INCREMENT"), ("UK", "title  VARCHAR(80)"), ("", "venue  VARCHAR(60)"), ("", "event_date  DATE")])
  + rel("student", "registration", "registers (1 : N)") + rel("event", "registration", "receives (1 : N)") + "}")

# ---------------- Task 1: class structure ----------------
render("class_task1", '''digraph G { rankdir=TB; node [shape=record, fontname="Helvetica", fontsize=11, style=filled, fillcolor="#F4F7FB"]; edge [fontname="Helvetica", fontsize=10];
 labelloc=t; label="Task 1 - repaired class structure (window only collects input and shows results)"; fontname="Helvetica-Bold";
 win [label="{FeeWindow (JFrame)|+ windowClosing()|creates FeePanel}", fillcolor="#DDE8F7"];
 panel [label="{FeePanel (JPanel)|- regNoField, amountField, status, receiptArea|- PayAction, ClearAction, ReceiptAction|+ runInBackground(work, onDone)}", fillcolor="#DDE8F7"];
 svc [label="{FeeService|+ pay(regNo, amountText) : PaymentResult|+ receipt(regNo) : String|(validation, bank, saving)}", fillcolor="#E2F0D9"];
 res [label="{PaymentResult|success, message, receiptText}"];
 bank [label="{\\<\\<interface\\>\\> BankGateway|+ confirm(regNo, amount)}"];
 sim [label="{SimulatedBank|delay}"];
 repo [label="{\\<\\<interface\\>\\> PaymentRepository|+ save(regNo, amount)|+ history(regNo)}", fillcolor="#FFF2CC"];
 jdbc [label="{JdbcPaymentRepository|PreparedStatement|try-with-resources}", fillcolor="#FFF2CC"];
 mem [label="{InMemoryPaymentRepository|(tests)}", fillcolor="#FFF2CC"];
 win -> panel [label="contains"]; panel -> svc [label="uses (in SwingWorker)"]; svc -> res [label="returns"];
 svc -> bank [label="uses"]; svc -> repo [label="uses"];
 sim -> bank [arrowhead=empty, style=dashed]; jdbc -> repo [arrowhead=empty, style=dashed]; mem -> repo [arrowhead=empty, style=dashed];
}''')

# ---------------- Task 2: containment tree + event map ----------------
render("tree_task2", '''digraph G { rankdir=TB; node [shape=box, fontname="Helvetica", fontsize=11, style="filled,rounded", fillcolor="#F4F7FB"]; edge [arrowhead=none];
 labelloc=t; label="Task 2 - containment hierarchy of the appointment window"; fontname="Helvetica-Bold";
 f [label="AppointmentWindow (JFrame)\\nWindowListener: windowClosing", fillcolor="#DDE8F7"];
 p [label="AppointmentPanel (JPanel, BorderLayout)", fillcolor="#DDE8F7"];
 form [label="form (JPanel, GridLayout 4 x 2)"]; south [label="south (JPanel, BorderLayout)"];
 l1 [label="JLabel Patient ID"]; t1 [label="JTextField patientId\\nDocumentListener", fillcolor="#FFF2CC"];
 l2 [label="JLabel Department"]; c1 [label="JComboBox departmentBox\\nItemListener", fillcolor="#FFF2CC"];
 l3 [label="JLabel Doctor"]; c2 [label="JComboBox doctorBox"]; l4 [label="JLabel Time slot"]; c3 [label="JComboBox slotBox"];
 row [label="buttonRow (JPanel, FlowLayout)"]; b [label="JButton Book appointment\\nActionListener", fillcolor="#FFF2CC"]; st [label="JLabel status"];
 f -> p; p -> form; p -> south; form -> l1; form -> t1; form -> l2; form -> c1; form -> l3; form -> c2; form -> l4; form -> c3;
 south -> row; row -> b; south -> st;
}''')
render("events_task2", '''digraph G { rankdir=LR; node [shape=box, fontname="Helvetica", fontsize=11, style="filled,rounded"]; edge [fontname="Helvetica", fontsize=10];
 labelloc=t; label="Task 2 - event sources, event objects and listeners"; fontname="Helvetica-Bold";
 s1 [label="Window close button", fillcolor="#DDE8F7"]; e1 [label="WindowEvent", fillcolor="#FFF2CC"]; l1 [label="WindowListener / WindowAdapter\\nwindowClosing()", fillcolor="#E2F0D9"];
 s2 [label="departmentBox", fillcolor="#DDE8F7"]; e2 [label="ItemEvent (SELECTED)", fillcolor="#FFF2CC"]; l2 [label="ItemListener\\nrefreshDoctors()", fillcolor="#E2F0D9"];
 s3 [label="Book button", fillcolor="#DDE8F7"]; e3 [label="ActionEvent", fillcolor="#FFF2CC"]; l3 [label="ActionListener\\nbook() -> SwingWorker", fillcolor="#E2F0D9"];
 s4 [label="patientId text field", fillcolor="#DDE8F7"]; e4 [label="DocumentEvent", fillcolor="#FFF2CC"]; l4 [label="DocumentListener\\n(form becomes 'in progress')", fillcolor="#E2F0D9"];
 s1 -> e1 -> l1; s2 -> e2 -> l2; s3 -> e3 -> l3; s4 -> e4 -> l4;
}''')

# ---------------- Task 3: nested containers ----------------
render("containers_task3", '''digraph G { rankdir=TB; compound=true; node [shape=box, fontname="Helvetica", fontsize=10, style="filled,rounded", fillcolor="#F4F7FB"]; edge [arrowhead=none];
 labelloc=t; label="Task 3 - container trees of the three designs"; fontname="Helvetica-Bold";
 subgraph cluster_a { label="Design A: nested Border + Flow (chosen)"; style=rounded; fontname="Helvetica-Bold";
   a0 [label="LibraryNested\\nBorderLayout", fillcolor="#DDE8F7"];
   a1 [label="north\\nBorderLayout", fillcolor="#DDE8F7"]; a2 [label="JScrollPane results\\n(CENTER: gets all spare space)", fillcolor="#E2F0D9"]; a3 [label="south\\nBorderLayout", fillcolor="#DDE8F7"];
   a4 [label="title JLabel\\n(NORTH)"]; a5 [label="searchRow\\nFlowLayout LEFT"]; a6 [label="buttonRow\\nFlowLayout CENTER"]; a7 [label="status JLabel\\n(SOUTH)"];
   a8 [label="label, query, category, Search"]; a9 [label="Prev, Next, Reserve"];
   a0 -> a1; a0 -> a2; a0 -> a3; a1 -> a4; a1 -> a5; a3 -> a6; a3 -> a7; a5 -> a8; a6 -> a9; }
 subgraph cluster_b { label="Design B: GridBagLayout"; style=rounded; fontname="Helvetica-Bold";
   b0 [label="LibraryGridBag\\nGridBagLayout", fillcolor="#DDE8F7"]; b1 [label="title (fill H)"]; b2 [label="label, query (weightx=1), category, Search"]; b3 [label="results (weightx=1, weighty=1, fill BOTH)", fillcolor="#E2F0D9"]; b4 [label="buttons panel (FlowLayout)"]; b5 [label="status"];
   b0 -> b1; b0 -> b2; b0 -> b3; b0 -> b4; b0 -> b5; }
 subgraph cluster_c { label="Design C: absolute (null layout)"; style=rounded; fontname="Helvetica-Bold";
   c0 [label="LibraryAbsolute\\nsetLayout(null)", fillcolor="#F8CBAD"]; c1 [label="10 components, each with fixed setBounds(x, y, w, h)"]; c0 -> c1; }
}''')

# ---------------- Task 4: event / paint flow ----------------
render("flow_task4", '''digraph G { rankdir=LR; node [shape=box, fontname="Helvetica", fontsize=11, style="filled,rounded"]; edge [fontname="Helvetica", fontsize=10];
 labelloc=t; label="Task 4 - why the dots disappear and how the repaired program keeps them"; fontname="Helvetica-Bold";
 subgraph cluster_bad { label="Original / step 2 (draw with getGraphics)"; style=rounded; fontname="Helvetica-Bold";
   c1 [label="mouse click", fillcolor="#DDE8F7"]; c2 [label="mouseClicked()\\ndraw dot on screen", fillcolor="#F8CBAD"]; c3 [label="minimise + restore\\n(pixels thrown away)", fillcolor="#FFF2CC"]; c4 [label="paint() redraws\\nNOTHING remembered", fillcolor="#F8CBAD"];
   c1 -> c2 -> c3 -> c4; }
 subgraph cluster_good { label="Repaired DrawPad (model + paint)"; style=rounded; fontname="Helvetica-Bold";
   d1 [label="mouse click", fillcolor="#DDE8F7"]; d2 [label="mouseClicked()\\npoints.add(p); repaint()", fillcolor="#E2F0D9"]; d3 [label="minimise + restore", fillcolor="#FFF2CC"]; d4 [label="paint() draws every\\nstored point", fillcolor="#E2F0D9"];
   d1 -> d2 -> d3 -> d4; d2 -> d4 [style=dashed, label="repaint()"]; }
}''')

# ---------------- Task 6 ----------------
render("arch_task6", '''digraph G { rankdir=TB; node [shape=box, fontname="Helvetica", fontsize=11, style="filled,rounded"]; edge [fontname="Helvetica", fontsize=10];
 labelloc=t; label="Task 6 - layered architecture (each layer talks only to the layer below it)"; fontname="Helvetica-Bold";
 subgraph cluster_ui { label="ui  (Swing)"; style=rounded; fillcolor="#EAF1FB"; style="rounded,filled";
   mf [label="MainFrame\\nJMenuBar + CardLayout", fillcolor="#DDE8F7"]; rp [label="RegisterPanel", fillcolor="#DDE8F7"]; lp [label="RegistrationsPanel\\nJTable + paging", fillcolor="#DDE8F7"]; }
 subgraph cluster_sv { label="service"; style="rounded,filled"; fillcolor="#EAF6E4"; sv [label="RegistrationService\\nvalidate - duplicate check - paging - CSV", fillcolor="#E2F0D9"]; }
 subgraph cluster_dao { label="dao"; style="rounded,filled"; fillcolor="#FFF8E1"; di [label="<<interface>> RegistrationDao", fillcolor="#FFF2CC"]; jd [label="JdbcRegistrationDao\\n(MySQL, PreparedStatement)", fillcolor="#FFF2CC"]; md [label="InMemoryRegistrationDao\\n(demo / tests)", fillcolor="#FFF2CC"]; }
 db [label="MySQL database\\nstudent - event - registration", shape=cylinder, fillcolor="#D9D9D9"];
 mod [label="model: Student, CampusEvent, RegistrationRow, PageData (records)", fillcolor="#F2F2F2"];
 mf -> rp; mf -> lp; rp -> sv [label="in SwingWorker"]; lp -> sv [label="in SwingWorker"]; mf -> sv [label="export CSV"];
 sv -> di; jd -> di [arrowhead=empty, style=dashed]; md -> di [arrowhead=empty, style=dashed]; jd -> db [label="JDBC"];
 sv -> mod [style=dotted, arrowhead=none];
}''')
render("flow_task6", '''digraph G { rankdir=LR; node [shape=box, fontname="Helvetica", fontsize=11, style="filled,rounded", fillcolor="#F4F7FB"]; edge [fontname="Helvetica", fontsize=10];
 labelloc=t; label="Task 6 - screen flow and menu structure"; fontname="Helvetica-Bold";
 start [label="start", shape=circle, fillcolor="#E2F0D9"]; reg [label="Register screen\\nform + Register / Clear", fillcolor="#DDE8F7"]; list [label="Registrations screen\\nJTable, filter, Prev / Next, Refresh", fillcolor="#DDE8F7"];
 exp [label="File > Export CSV...\\n(JFileChooser, SwingWorker)", fillcolor="#FFF2CC"]; about [label="Help > About\\n(JOptionPane)", fillcolor="#FFF2CC"]; ex [label="File > Exit / close button\\n(ask if work is running)", fillcolor="#F8CBAD"];
 start -> reg; reg -> list [label="View menu / after a registration"]; list -> reg [label="View menu"]; list -> exp; reg -> about; list -> about; reg -> ex; list -> ex;
}''')
render("threads_task6", '''digraph G { rankdir=LR; node [shape=box, fontname="Helvetica", fontsize=11, style="filled,rounded"]; edge [fontname="Helvetica", fontsize=10];
 labelloc=t; label="Task 6 - which thread does what (the event-dispatch thread never waits for the database)"; fontname="Helvetica-Bold";
 subgraph cluster_edt { label="Event-dispatch thread (EDT)"; style="rounded,filled"; fillcolor="#EAF1FB";
   a [label="button click\\nread text fields\\ndisable button, show 'Saving ...'", fillcolor="#DDE8F7"]; d [label="done():\\nshow message, update JTable,\\nre-enable button", fillcolor="#DDE8F7"]; }
 subgraph cluster_w { label="SwingWorker thread"; style="rounded,filled"; fillcolor="#EAF6E4";
   w [label="doInBackground():\\nservice.register(...) / loadPage(...)\\nJDBC call to MySQL", fillcolor="#E2F0D9"]; }
 a -> w [label="execute()"]; w -> d [label="result via get()"];
}''')
print("diagrams done")

render("class_task6", """digraph G { rankdir=TB; node [shape=record, fontname="Helvetica", fontsize=10, style=filled, fillcolor="#F4F7FB"]; edge [fontname="Helvetica", fontsize=9];
 labelloc=t; label="Task 6 - class diagram: user interface, service and data-access (JDBC) layers"; fontname="Helvetica-Bold";
 mf [label="{MainFrame (JFrame)|- cards : CardLayout|- menuBar : JMenuBar|+ showScreen(name)|~ requestExit()|- exportCsv()}", fillcolor="#DDE8F7"];
 rp [label="{RegisterPanel (JPanel)|- regNo, name, email : JTextField|- eventBox : JComboBox|+ register() [SwingWorker]|+ isBusy()}", fillcolor="#DDE8F7"];
 lp [label="{RegistrationsPanel (JPanel)|- table : JTable|- filterBox : JComboBox|- prev, next, refresh : JButton|+ load(pageNo) [SwingWorker]}", fillcolor="#DDE8F7"];
 sv [label="{RegistrationService|+ events() : List|+ register(regNo, name, email, event) : ServiceResult|+ loadPage(filter, pageNo, size) : PageData|+ exportCsv(file, filter) : int}", fillcolor="#E2F0D9"];
 di [label="{\\<\\<interface\\>\\> RegistrationDao|+ findEvents()|+ exists(regNo, eventId)|+ register(student, eventId)|+ count(eventId)|+ page(eventId, limit, offset)|+ findAll(eventId)}", fillcolor="#FFF2CC"];
 jd [label="{JdbcRegistrationDao|- url, user, password|PreparedStatement, transaction,\\nSQL error 1062 = duplicate}", fillcolor="#FFF2CC"];
 md [label="{InMemoryRegistrationDao|lists + HashSet key\\n(demo, tests, benchmarks)}", fillcolor="#FFF2CC"];
 m1 [label="{Student|regNo, name, email}"]; m2 [label="{CampusEvent|id, title, venue, date}"]; m3 [label="{RegistrationRow|regId, regNo, studentName,\\neventTitle, registeredOn}"]; m4 [label="{PageData|rows, totalRows, pageNo, pageCount}"];
 ex [label="{DuplicateRegistrationException|(checked)}"]; ex2 [label="{DataAccessException|(runtime)}"];
 mf -> rp [label="1"]; mf -> lp [label="1"]; rp -> sv [label="uses"]; lp -> sv [label="uses"]; mf -> sv [label="uses"];
 sv -> di [label="uses"]; jd -> di [arrowhead=empty, style=dashed]; md -> di [arrowhead=empty, style=dashed];
 di -> ex [style=dotted, label="throws"]; di -> ex2 [style=dotted, label="throws"];
 sv -> m4 [style=dotted]; di -> m1 [style=dotted]; di -> m2 [style=dotted]; di -> m3 [style=dotted];
}""")
print("class_task6 done")
