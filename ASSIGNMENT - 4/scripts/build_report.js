// Builds report/CSA09_Assignment4_Report.docx. The uploaded assignment pages come FIRST, then the answers.
const fs = require('fs');
const path = require('path');
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun, HeadingLevel, AlignmentType, WidthType,
  ShadingType, BorderStyle, LevelFormat, Footer, PageNumber, SectionType } = require('docx');

const ROOT = path.resolve(__dirname, '..');
const rd = f => fs.readFileSync(path.join(ROOT, f), 'utf8');
function csv(file) {
  const lines = rd(file).trim().split('\n'); const head = lines[0].split(',');
  return lines.slice(1).map(l => { const c = l.split(','); const o = {}; head.forEach((h, i) => o[h] = c[i]); return o; });
}
const f1 = x => Number(x).toFixed(1), f2 = x => Number(x).toFixed(2), f3 = x => Number(x).toFixed(3);
const fmt = x => Number(x).toLocaleString('en-US');

const runs = (text, opts = {}) => text.split(/(\*\*[^*]+\*\*)/).filter(s => s.length).map(s =>
  s.startsWith('**') ? new TextRun({ text: s.slice(2, -2), bold: true, ...opts }) : new TextRun({ text: s, ...opts }));
const P = (text, opts = {}) => new Paragraph({ children: runs(text), spacing: { after: 110, line: 276 }, ...opts });
const H1 = t => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)], pageBreakBefore: true });
const H2 = t => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const H3 = t => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(t)] });
const B = text => new Paragraph({ numbering: { reference: 'bul', level: 0 }, children: runs(text), spacing: { after: 60, line: 276 } });
const caption = t => new Paragraph({ children: [new TextRun({ text: t, italics: true, size: 18 })], alignment: AlignmentType.CENTER, spacing: { after: 160 } });
const gap = () => new Paragraph({ children: [], spacing: { after: 100 } });
const codeLines = (txt, size = 15, fill = 'F2F2F2') => txt.replace(/\s+$/, '').split('\n').map(l => new Paragraph({
  children: [new TextRun({ text: l.length ? l.replace(/\t/g, '    ') : ' ', font: 'Courier New', size })],
  shading: { type: ShadingType.CLEAR, fill }, spacing: { after: 0, line: 220 }, indent: { left: 100, right: 100 } }));
const code = txt => codeLines(txt.replace(/^\n/, ''));
// a source file from the repository, with a small title line
const codeFile = (file, note) => [new Paragraph({ children: [new TextRun({ text: file + (note ? '   -   ' + note : ''), bold: true, size: 18, color: '2F5496' })], spacing: { before: 140, after: 40 }, keepNext: true }), ...codeLines(rd(file))];
// a captured console output
const outFile = (file, title) => [new Paragraph({ children: [new TextRun({ text: title || file, bold: true, size: 18, color: '375623' })], spacing: { before: 120, after: 40 }, keepNext: true }), ...codeLines(rd(file), 14, 'E8F0E0')];

const border = { style: BorderStyle.SINGLE, size: 4, color: 'AAAAAA' };
const borders = { top: border, bottom: border, left: border, right: border };
function table(headers, rows, widths, opts = {}) {
  const total = widths.reduce((a, b) => a + b, 0), size = opts.size || 17;
  const mk = (txt, w, head, align) => new TableCell({ borders, width: { size: w, type: WidthType.DXA },
    shading: head ? { type: ShadingType.CLEAR, fill: 'D9E2F3' } : undefined, margins: { top: 50, bottom: 50, left: 80, right: 80 },
    children: String(txt).split('\n').map(line => new Paragraph({ alignment: align, children: runs(line, { size, bold: head }) })) });
  const trs = [new TableRow({ tableHeader: true, children: headers.map((h, i) => mk(h, widths[i], true, AlignmentType.LEFT)) })];
  rows.forEach(r => trs.push(new TableRow({ cantSplit: true, children: r.map((c, i) => mk(c, widths[i], false, (i === 0 || opts.leftAll) ? AlignmentType.LEFT : AlignmentType.RIGHT)) })));
  return new Table({ width: { size: total, type: WidthType.DXA }, columnWidths: widths, rows: trs });
}
function img(file, maxW = 540, maxH = 700) {
  const buf = fs.readFileSync(path.join(ROOT, file));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  const sc = Math.min(maxW / w, maxH / h, 1.6);
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 80, after: 40 }, keepNext: true,
    children: [new ImageRun({ type: 'png', data: buf, transformation: { width: Math.round(w * sc), height: Math.round(h * sc) }, altText: { title: file, description: file, name: file } })] });
}
const fig = (file, maxW, maxH, cap) => [img(file, maxW, maxH), caption(cap)];
const wordCount = s => s.trim().split(/\s+/).length;

// ---------------------------------------------------------------- SECTION 0: the assignment as issued (first pages)
const assignmentPages = [];
for (let i = 1; i <= 9; i++) {
  const buf = fs.readFileSync(path.join(ROOT, `assignment/pg-${i}.png`));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  const W = 740, Hh = Math.round(W * h / w);
  assignmentPages.push(new Paragraph({ spacing: { before: 0, after: 0 }, alignment: AlignmentType.CENTER,
    children: [new ImageRun({ type: 'png', data: buf, transformation: { width: W, height: Math.min(Hh, 1066) }, altText: { title: 'Assignment page ' + i, description: 'Assignment 4 page ' + i, name: 'assignment' + i } })] }));
}

const body = [];
const add = (...x) => x.forEach(e => Array.isArray(e) ? e.forEach(y => body.push(y)) : body.push(e));

// ---------------------------------------------------------------- COVER + FRONT MATTER
add(
  new Paragraph({ spacing: { before: 1500 }, alignment: AlignmentType.CENTER, children: [new TextRun({ text: '<YOUR COLLEGE / DEPARTMENT NAME>', bold: true, size: 28 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 500 }, children: [new TextRun({ text: '(write your college and department here)', italics: true, size: 20 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: [new TextRun({ text: 'CSA09 – PROGRAMMING IN JAVA', bold: true, size: 40 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: [new TextRun({ text: 'ASSIGNMENT 4 – CO4 REPORT', bold: true, size: 34 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 }, children: [new TextRun({ text: 'Event-Driven GUI Design (AWT, Swing, Layouts, Listeners, JDBC)', size: 24 })] }),
  table(['Item', 'Details'], [
    ['Student name', '<YOUR NAME>'], ['Register number', '<REGISTER NUMBER>'],
    ['Course', 'CSA09 – Programming in Java'], ['Assignment', 'Assignment 4 – CO4 (Sessions 49–56), Individual, 50 marks'],
    ['Java version used', 'OpenJDK 21 (java -version)'],
    ['GitHub repository', 'https://github.com/<your-username>/CSA09-<RegisterNumber>-Assignment4'],
    ['Submission date', '<DATE>'],
  ], [2600, 6426], { leftAll: true, size: 21 }),
  gap(),
  P('The pages before this cover page are the assignment as issued. The answers follow in the same order as the tasks: Task 1 to Task 6, each with the diagnosis or design, the complete source code, the captured program output, the figures and the alternative-concept analysis. ER diagrams are given for the three tasks that use a database (Tasks 1, 2 and 6).'),
  H1('Contents'),
  table(['Part', 'Topic', 'Marks'], [
    ['Task 1', 'Diagnose and repair a poor event-handling design (fee-payment window)', '6'],
    ['Task 2', 'From requirement to event-driven design (outpatient appointment desk)', '8'],
    ['Task 3', 'From sketch to layout (campus library search screen)', '9'],
    ['Task 4', 'Event-handling forensics (drawing pad) – with Complete the Code', '7'],
    ['Task 5', 'Same screen, different event-handling styles (currency converter) – with Complete the Code', '7'],
    ['Task 6', 'Campus event registration desktop application (Swing + JDBC + MySQL)', '13'],
    ['Appendix A', 'Repository layout, how to compile and run, GitHub commit steps', '–'],
    ['Appendix B', 'Test harness and headless shims (how the screenshots in this report were produced)', '–'],
    ['Appendix C', 'Requirement-to-evidence traceability table and viva preparation', '–'],
  ], [1400, 6626, 1000], { leftAll: true, size: 20 }),
  gap(),
  H2('How the evidence in this report was produced (please read)'),
  P('Every program in this report was compiled with javac and executed. However, the machine used to prepare this report is a server **without a screen** and without a MySQL server, so some evidence was produced in a different way from a normal desktop run. This is stated here and again under each figure, so that nothing is presented as something it is not.'),
  table(['Evidence', 'How it was produced', 'What it proves / what it does not prove'], [
    ['Console output of services, tests, benchmarks', 'Plain java commands; captured text is pasted unchanged.', 'Real executions. Timings depend on the machine; trends and operation counts do not.'],
    ['Screenshots of windows ("off-screen render")', 'A test harness runs the real window classes in a headless JVM, sends real events (button clicks, Enter, mouse clicks, combo-box selections) and paints the real Swing component tree into a PNG with Swing\'s own painting code. A small test-only shim (Appendix B) stands in for the native window.', 'The layout, the components, the text and the event behaviour are the real ones. The operating-system window frame, fonts and look-and-feel may differ from a real desktop, and "minimise / restore" is imitated by asking the window to paint itself again.'],
    ['AWT window events (windowOpened, windowActivated ...)', 'Documented sequence (Task 2.4). The program WindowTrace.java is provided to print the real sequence.', 'Not executed here because a real window is needed.'],
    ['MySQL / JDBC (Tasks 1, 2, 6)', 'JDBC classes are written and compile-checked. The application runs here with an in-memory DAO. The SQL script was checked with SQLite as a stand-in (Task 6).', 'The SQL logic (constraints, joins, paging) is verified. The MySQL driver, server and error texts are not.'],
  ], [2300, 3700, 3026], { leftAll: true, size: 16 }),
  gap(),
  P('**For the final submission:** run every program on your own computer (the assignment requires individual execution), retake the screenshots from the real windows, run the MySQL script and the JDBC version of Task 6, and replace the placeholders. The programs, tests and analysis do not need to change.'),
);

// =====================================================================================================
// TASK 1
// =====================================================================================================
const d1 = csv('Task01/dispatch_results.csv');
const dpb = (d1[4].own_listener_bytes - d1[3].own_listener_bytes) / (10000 - 1000);
const defence = 'The window now only reads text fields and displays a PaymentResult, so it holds no business rules. FeeService has no Swing code, so twelve automatic tests (valid, empty, text, negative, injection) run in milliseconds without a window or database, using an in-memory repository. Database access sits behind an interface, so MySQL can be replaced without touching the service or the window. Each button has its own small Action, so changing Pay cannot break Clear or Receipt. Slow work runs in a SwingWorker, so the window stays responsive.';
if (wordCount(defence) > 100) throw new Error('defence too long: ' + wordCount(defence));

add(
  H1('Task 1 – GUI Design Review: Diagnose and Repair a Poor Event-Handling Design'),
  P('**Bloom\'s level K4 – Analyse. 6 marks.** The junior developer\'s fee-payment window handles every button in one listener and keeps all program logic inside it. The original code (as given) is reproduced below.'),
  ...codeFile('Task01/FeeWindowBad.java.txt', 'original code from the assignment'),
  H2('Task 1.1 – Diagnose the design (ten deficiencies)'),
  table(['#', 'Deficiency (where)', 'What the user experiences / what can go wrong'], [
    ['D1', 'Interface and logic are mixed. actionPerformed() contains input parsing, JDBC, the bank wait and the console message.', 'Nothing can be tested without opening a window and a real database. Any change (a new rule, another database) means editing the same method, and one mistake breaks every button.'],
    ['D2', 'The action command is compared with ==  (e.getActionCommand() == "Pay").', '== compares references, not text. It works only while both strings are the same interned literal. If the label comes from a resource file, a translation or new String(...), the test is false and clicking Pay does nothing. (Shown in the test output below.)'],
    ['D3', 'Everything that is not "Pay" goes to the else branch.', 'Clicking Receipt runs the Clear code: the fields are wiped and nothing is shown. The Receipt button is not implemented at all, and the user loses what was typed.'],
    ['D4', 'No validation of the input. Double.parseDouble(amount.getText()) is called on raw text.', 'An empty or non-numeric amount throws NumberFormatException on the event thread: a stack trace on the console, and the window shows nothing. Negative, zero or absurd amounts and any register number are accepted and stored.'],
    ['D5', 'Database work and Thread.sleep(3000) run on the event-dispatch thread.', 'The window freezes for the whole payment: it cannot repaint, the button stays half-pressed, the window cannot be moved or closed ("Not responding"). A slow database makes it worse.'],
    ['D6', 'SQL built by string concatenation ("... VALUES(\'" + regNo.getText() + ...).', 'SQL injection: a register number such as  x\', 0); DROP TABLE fees; --  changes the statement. Even honest text containing an apostrophe breaks it. The column list is also missing, so any change of the table breaks the statement.'],
    ['D7', 'The Connection (and Statement) are never closed.', 'Every payment leaks a connection. After enough payments the database refuses new connections and the whole college stops being able to pay.'],
    ['D8', 'Checked exceptions (SQLException, InterruptedException) are not handled.', 'As written the code does not compile; when "fixed" with a blanket throws or an empty catch, errors disappear silently and the user does not know whether the money was taken.'],
    ['D9', 'Feedback is System.out.println("Paid") only.', 'The receptionist looking at the window sees no confirmation and no error. "Paid" is printed even though the "bank confirmation" is only a sleep.'],
    ['D10', 'Closing is not handled (no WindowListener); a syntax error (missing +) is present.', 'An AWT Frame ignores the close button, so the window cannot be closed normally and a connection that is still open is never released.'],
  ], [600, 3600, 4826], { leftAll: true, size: 16 }),
  caption('Table 1.1 – Diagnosis (D1–D10). The design deficiencies are D1–D9; D10 includes the syntax error that stops the original from compiling.'),
  H2('Task 1.2 – Repair the design'),
  P('**Class structure.** The window now only collects input and shows results. A separate FeeService validates the input and performs the payment. The service does not know Swing, and it does not know JDBC: it talks to two interfaces (PaymentRepository and BankGateway) so that the database and the bank can be replaced.'),
  ...fig('diagrams/class_task1.png', 520, 520, 'Figure 1.1 – Class structure of the repaired design'),
  table(['Class', 'Responsibility', 'Repairs'], [
    ['FeeWindow (JFrame)', 'Creates the panel; handles window closing (asks if a payment is running).', 'D10'],
    ['FeePanel (JPanel)', 'Text fields, three buttons (one Action each), status line, receipt area. Starts SwingWorkers.', 'D1, D2, D3, D5, D9'],
    ['FeeService', 'Validation, bank confirmation, saving, receipt text. Returns a PaymentResult; never throws to the window.', 'D1, D4, D8'],
    ['PaymentRepository / JdbcPaymentRepository / InMemoryPaymentRepository', 'Data access. JDBC version uses PreparedStatement and try-with-resources.', 'D6, D7'],
    ['BankGateway / SimulatedBank', 'The slow bank call, replacing Thread.sleep in the listener.', 'D5'],
  ], [3000, 4700, 1326], { leftAll: true, size: 16 }),
  P('**Event handling – before and after.**'),
  ...code(`
// BEFORE: one listener, string compare with ==, everything inside, Receipt = Clear
public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand() == "Pay") { ... JDBC ... Thread.sleep(3000); ... } else { clear fields }
}

// AFTER: one small Action per button, no if-else chain, slow work in a SwingWorker
buttons.add(new JButton(payAction));        // PayAction     -> service.pay(...)      in the background
buttons.add(new JButton(clearAction));      // ClearAction   -> clears fields only
buttons.add(new JButton(receiptAction));    // ReceiptAction -> service.receipt(...)  in the background`),
  gap(),
  H3('Complete source code of the repaired design'),
  ...codeFile('Task01/PaymentResult.java'), ...codeFile('Task01/PaymentRepository.java'),
  ...codeFile('Task01/JdbcPaymentRepository.java', 'PreparedStatement, try-with-resources'),
  ...codeFile('Task01/InMemoryPaymentRepository.java'), ...codeFile('Task01/BankGateway.java'), ...codeFile('Task01/SimulatedBank.java'),
  ...codeFile('Task01/FeeService.java', 'validation + payment'), ...codeFile('Task01/FeePanel.java', 'the rewritten event handling'),
  ...codeFile('Task01/FeeWindow.java'), ...codeFile('Task01/FeeServiceTest.java', 'automatic tests'),
  ...codeFile('Task01/schema.sql', 'MySQL tables'),
  H3('Database design (ER diagram)'),
  ...fig('diagrams/er_task1.png', 460, 330, 'Figure 1.2 – ER diagram of the fee database (one student pays many times)'),
  H3('Evidence 1 – automatic tests of FeeService (each test is linked to the deficiency it repairs)'),
  ...outFile('Task01/feeservice_test_output.txt', 'Output of: java FeeServiceTest'),
  table(['Test (output line)', 'Deficiency repaired', 'What it shows'], [
    ['empty register number, short register number, null inputs', 'D4', 'Invalid input is rejected with a clear message and never stored.'],
    ['amount is text / negative / zero / too large / NaN', 'D4', 'Amount rules are enforced; no NumberFormatException reaches the user.'],
    ['SQL injection in reg number', 'D6', 'Rejected by validation; and the repository stores values as data (PreparedStatement).'],
    ['"Payments stored after all tests: 2"', 'D4, D6', 'Only the 2 valid payments reached the repository.'],
    ['"Pay" == "Pay" vs new String("Pay") == "Pay"', 'D2', 'Reference comparison is false for an equal string; equals() is true.'],
    ['Original builds ... / Repaired code sends ...', 'D6', 'The original statement is rewritten by the input; the repaired statement keeps the input as data only.'],
  ], [3300, 1700, 4026], { leftAll: true, size: 16 }),
  H3('Evidence 2 – the repaired window, driven by real events (off-screen render)'),
  ...outFile('Task01/feewindow_ui_output.txt', 'Output of the harness FeeHarness (Appendix B)'),
  ...fig('screenshots/task01/t1_01_initial.png', 400, 300, 'Figure 1.3 – Initial window'),
  ...fig('screenshots/task01/t1_02_empty_error.png', 400, 300, 'Figure 1.4 – Demonstrates D4/D9: Pay with empty fields shows a clear message'),
  ...fig('screenshots/task01/t1_03_invalid_amount.png', 400, 300, 'Figure 1.5 – Demonstrates D4: amount "abc" is rejected without an exception'),
  ...fig('screenshots/task01/t1_04_success.png', 400, 300, 'Figure 1.6 – Demonstrates D5/D9: payment accepted after the 1.5 s bank call; confirmation and receipt shown'),
  ...fig('screenshots/task01/t1_05_receipt.png', 400, 300, 'Figure 1.7 – Demonstrates D3: the Receipt button shows the receipts instead of clearing the form'),
  P('**D5 evidence:** the bank call took 1.5 s. During that time the program asked the event-dispatch thread to do a tiny task 32 times, and the slowest answer took 1.28 ms (see the output above). With the original code the same request would have waited the whole 3 seconds.'),
  H2('Task 1.3 – Design defence (' + wordCount(defence) + ' words)'),
  P(defence),
  H2('Alternative concept and complexity analysis'),
  P('**The alternative solution.** Instead of one listener with an if–else chain on the action command, every button gets its own listener object (or a Swing Action). This is what the repaired FeePanel already does. The two solutions in code:'),
  ...code(`
// one listener, if-else chain on the command (k comparisons in the worst case)
public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals("Pay")) { ... } else if (cmd.equals("Clear")) { ... } else if (cmd.equals("Receipt")) { ... }  // ... k branches
}

// one listener (Action) per button: the event goes straight to the right object
payButton.addActionListener(e -> pay());          // or  new JButton(payAction)
clearButton.addActionListener(e -> clear());
receiptButton.addActionListener(e -> receipt());`),
  gap(),
  table(['', 'One listener + if-else chain', 'One listener / Action per button'], [
    ['Time to dispatch one event (k buttons)', 'O(k) comparisons in the worst case (O(1) for the first branch)', 'O(1): Swing calls the listener registered on the button that fired'],
    ['Space', 'One listener object + k command strings', 'k small listener objects (about 20 bytes each) + 0 command strings'],
    ['Adding a button', 'Edit the shared method (risk to the others)', 'Add one registration; nothing else changes'],
  ], [2400, 3300, 3326], { leftAll: true, size: 16 }),
  P('**Measurements** (DispatchBenchmark.java; time for the LAST button, the worst case; average of 5 runs after warm-up):'),
  table(['k buttons', 'if-else chain (ns/event)', 'HashMap lookup (ns/event)', 'own listener (ns/event)', 'bytes: k listeners'],
    d1.map(r => [fmt(r.k), f1(r.chain_ns_per_event), f1(r.map_ns_per_event), f1(r.own_listener_ns_per_event), fmt(r.own_listener_bytes)]), [1500, 2000, 2000, 1800, 1726], { size: 16 }),
  caption('Table 1.2 – Dispatch time and listener memory (from Task01/dispatch_results.csv)'),
  ...fig('graphs/task1_dispatch.png', 420, 300, 'Figure 1.8 – Dispatch time against the number of buttons k (logarithmic axes)'),
  P(`**Analysis.** The chain grows with k: from ${f1(d1[0].chain_ns_per_event)} ns (k = 3) to ${fmt(Math.round(d1[4].chain_ns_per_event))} ns (k = 10,000), i.e. about 1,100 times for 3,333 times more buttons, which is the linear growth O(k). The HashMap and the own-listener approaches stay flat (O(1)); the first HashMap value (${f1(d1[0].map_ns_per_event)} ns) is warm-up noise, and the own-listener time of about 0.1 ns means the JIT compiler inlined the call completely. The space for k separate listeners is about ${dpb.toFixed(0)} bytes each (a 16-byte object and a 4-byte array slot), i.e. ${fmt(d1[4].own_listener_bytes)} bytes for k = 10,000. **Conclusion:** the O(k) against O(1) difference is real, but for a window with 3–20 buttons both costs are far below one microsecond and 1 KB. The decisive reasons for the redesign are testability and maintainability (D1, D2, D3), not the speed of dispatch.`),
);

// =====================================================================================================
// TASK 2
// =====================================================================================================
const c2 = csv('Task02/cache_results.csv');
add(
  H1('Task 2 – From Requirement to Event-Driven Design: Hospital Outpatient Appointment Desk'),
  P('**Bloom\'s level K4 – Analyse. 8 marks.** The receptionist enters a patient ID, selects a department, chooses a doctor (the doctors shown depend on the department), picks a time slot and books. The window shows a confirmation or an error and asks for confirmation before closing if a booking is in progress.'),
  H2('Task 2.1 – Model the events'),
  table(['Event source', 'Event object generated', 'Listener interface', 'Action taken'], [
    ['Window (title-bar close button)', 'WindowEvent (WINDOW_CLOSING)', 'WindowListener (via WindowAdapter)', 'If a booking is in progress ask “Close anyway?”; if yes dispose(), otherwise stay open.'],
    ['Window (when first shown)', 'WindowEvent (WINDOW_OPENED, WINDOW_ACTIVATED)', 'WindowListener', 'Not needed for the logic; used for the trace in Task 2.4.'],
    ['Department JComboBox', 'ItemEvent (SELECTED, and DESELECTED for the old item)', 'ItemListener', 'Only for SELECTED: clear the doctor list and fill it with the doctors of the chosen department from the cached Map.'],
    ['Patient-ID JTextField (typing)', 'DocumentEvent (insert / remove)', 'DocumentListener', 'The form becomes “in progress” (used when closing). (KeyEvent / KeyListener would also work but sees keys, not text changes.)'],
    ['Book JButton (click, or Enter on the button)', 'ActionEvent', 'ActionListener', 'Validate and book in a SwingWorker; show the confirmation or the error; clear the form on success.'],
    ['SwingWorker (booking finished)', 'done() callback on the event-dispatch thread', '(SwingWorker)', 'Show the result, re-enable the button.'],
  ], [2100, 2300, 1900, 2726], { leftAll: true, size: 16 }),
  caption('Table 2.1 – Event model'),
  ...fig('diagrams/events_task2.png', 560, 420, 'Figure 2.1 – Event sources, event objects and listeners'),
  H2('Task 2.2 – Component hierarchy'),
  ...fig('diagrams/tree_task2.png', 560, 460, 'Figure 2.2 – Containment tree with the Swing class of every node'),
  table(['Node', 'Swing class', 'Layout manager'], [
    ['Top-level window', 'JFrame (AppointmentWindow)', 'BorderLayout (content pane)'],
    ['Whole form', 'JPanel (AppointmentPanel)', 'BorderLayout: form in CENTER, south panel in SOUTH'],
    ['Four label/field rows', 'JPanel “form”', 'GridLayout 4 × 2 (equal rows, label left, field right)'],
    ['Patient ID / Department / Doctor / Time slot', 'JTextField, JComboBox<String>, JComboBox<Doctor>, JComboBox<String>', '–'],
    ['Button row + status', 'JPanel (FlowLayout, centred) with a JButton; JLabel', 'BorderLayout (button row NORTH, status SOUTH)'],
  ], [3000, 3600, 2426], { leftAll: true, size: 16 }),
  H2('Task 2.3 – Handler logic (pseudocode)'),
  P('The pseudocode below uses numbered steps with the keywords ALGORITHM, INPUT, OUTPUT, BEGIN, END, IF/THEN/ELSE/ENDIF, FOR EACH/ENDFOR, CALL, SET and RETURN. (The text of the “SIMATS Engineering – Standard for Algorithmic Pseudocode” was not available while writing; if it prescribes different keywords, only the keywords need to be changed.)'),
  ...code(`
ALGORITHM RefreshDoctorList
INPUT   : selectedDepartment, doctorsByDepartment (Map from department name to list of Doctor)
OUTPUT  : the doctor combo box shows only the doctors of the selected department
BEGIN
  1.  CALL doctorBox.removeAllItems()
  2.  IF selectedDepartment is the prompt "-- select --" THEN
  3.      RETURN                                        // nothing to show
  4.  ENDIF
  5.  SET doctors <- doctorsByDepartment.get(selectedDepartment)     // empty list if the key is missing
  6.  FOR EACH doctor IN doctors DO
  7.      CALL doctorBox.addItem(doctor)
  8.  ENDFOR
  9.  CALL statusLabel.setText("")
END`),
  gap(),
  ...code(`
ALGORITHM ValidateAndBook
INPUT   : patientId, department, doctor, slot
OUTPUT  : a confirmation or an error message on the status label
BEGIN
  1.  SET id <- trim(patientId)
  2.  IF id is empty THEN  RETURN error "Enter the patient ID"                    ENDIF
  3.  IF id does not match the pattern P followed by 4 digits THEN  RETURN error "Patient ID must look like P1001"  ENDIF
  4.  IF department is not selected THEN  RETURN error "Select a department"      ENDIF
  5.  IF doctor is not selected THEN  RETURN error "Select a doctor"              ENDIF
  6.  IF doctor does not belong to department THEN  RETURN error                  ENDIF
  7.  IF slot is not selected THEN  RETURN error "Select a time slot"             ENDIF
  8.  IF repository.isSlotTaken(doctor, slot) THEN  RETURN error "already booked" ENDIF
  9.  CALL repository.save(id, doctor, slot)
  10. CALL resetForm()                                // so the window is no longer "in progress"
  11. RETURN confirmation "Appointment confirmed: id with doctor at slot"
END                                                  // steps 1-11 run in a SwingWorker, not on the event-dispatch thread`),
  gap(),
  H2('Task 2.4 – Trace the event sequence'),
  P('The sequence below is taken from the documented behaviour of the AWT/Swing window events (it requires a real window, so it was not executed on the server used for this report). The file Task02/WindowTrace.java prints the real order; run it, minimise, restore and close the window, and paste the output here to confirm.'),
  table(['Step', 'What happens', 'Methods called'], [
    ['1', 'main() starts; the window object is created on the event-dispatch thread', 'AppointmentWindow constructor → JFrame constructor → AppointmentPanel constructor (components created, listeners registered) → pack() → setLocationRelativeTo()'],
    ['2', 'setVisible(true): the window appears', 'addNotify() (native peer) → paint() / paintComponent() → windowOpened() → windowActivated() → windowGainedFocus()'],
    ['3', 'The user types the patient ID and selects a department', 'DocumentListener.insertUpdate() for each character; ItemListener.itemStateChanged() (DESELECTED, then SELECTED) → refreshDoctors()'],
    ['4', 'The user minimises the window', 'windowIconified() → windowDeactivated()   (windowStateChanged() on the state listener)'],
    ['5', 'The user restores it', 'windowDeiconified() → windowActivated() → paint() (the whole window is painted again)'],
    ['6', 'The user clicks Book', 'ActionListener.actionPerformed() → SwingWorker.execute() → doInBackground() (worker thread) → done() (event-dispatch thread)'],
    ['7', 'The user clicks the close button', 'windowClosing() → confirmClose() → JOptionPane.showConfirmDialog() → (if yes) dispose() → windowDeactivated() → windowClosed()'],
  ], [700, 3000, 5326], { leftAll: true, size: 16 }),
  caption('Table 2.2 – Order of calls from open to close'),
  H3('Comparison with the life-cycle of an applet'),
  table(['Applet method', 'When it is called', 'Counterpart in the window application'], [
    ['init()', 'Once, when the applet is loaded', 'The constructor (create components, register listeners)'],
    ['start()', 'When the applet becomes visible / the page is shown again', 'setVisible(true) → windowOpened(); windowDeiconified() / windowActivated() when restored'],
    ['paint(Graphics)', 'Whenever the area must be drawn', 'paint() / paintComponent() – same idea, called by the system'],
    ['stop()', 'When the page is left (the applet should pause)', 'windowIconified() / windowDeactivated()'],
    ['destroy()', 'Once, when the applet is removed', 'windowClosing() → dispose() → windowClosed()'],
  ], [1800, 3300, 3926], { leftAll: true, size: 16 }),
  P('The applet life cycle is driven by the browser; a window application is driven by the window system through WindowEvents, and the program decides in windowClosing() whether the window really closes. (The Applet API is deprecated in current Java versions, which is another reason to use JFrame.)'),
  H2('Implementation'),
  ...codeFile('Task02/Doctor.java'), ...codeFile('Task02/AppointmentRepository.java'), ...codeFile('Task02/InMemoryAppointmentRepository.java'),
  ...codeFile('Task02/JdbcAppointmentRepository.java', 'MySQL version'), ...codeFile('Task02/BookingResult.java'),
  ...codeFile('Task02/AppointmentService.java', 'doctors cached once in a Map<String, List<Doctor>>'),
  ...codeFile('Task02/AppointmentPanel.java', 'listeners for ItemEvent, ActionEvent, DocumentEvent'),
  ...codeFile('Task02/AppointmentWindow.java', 'WindowAdapter + confirmation before closing'),
  ...codeFile('Task02/WindowTrace.java', 'run on a real display to print the real window-event order'),
  ...codeFile('Task02/AppointmentServiceTest.java'), ...codeFile('Task02/schema.sql'),
  H3('Database design (ER diagram)'),
  ...fig('diagrams/er_task2.png', 560, 360, 'Figure 2.3 – ER diagram of the hospital database (the UNIQUE key on doctor, date and slot prevents double booking)'),
  H3('Evidence – service tests'),
  ...outFile('Task02/appointment_service_test_output.txt', 'Output of: java AppointmentServiceTest'),
  H3('Evidence – the real window classes driven by real events (off-screen render)'),
  ...outFile('Task02/appointment_ui_output.txt', 'Output of the harness AppointmentHarness (includes the list of events handled, in order)'),
  ...fig('screenshots/task02/t2_01_initial.png', 400, 300, 'Figure 2.4 – Initial window: the doctor list is empty until a department is chosen'),
  ...fig('screenshots/task02/t2_02_cardiology.png', 400, 300, 'Figure 2.5 – Demonstrates RefreshDoctorList: Cardiology selected → its three doctors'),
  ...fig('screenshots/task02/t2_03_paediatrics.png', 400, 300, 'Figure 2.6 – Department changed to Paediatrics → the list is replaced'),
  ...fig('screenshots/task02/t2_04_error.png', 400, 300, 'Figure 2.7 – Demonstrates ValidateAndBook step 2: empty patient ID'),
  ...fig('screenshots/task02/t2_05_success.png', 400, 300, 'Figure 2.8 – Demonstrates ValidateAndBook step 11: confirmation shown, form cleared'),
  ...fig('screenshots/task02/t2_06_slot_taken.png', 400, 300, 'Figure 2.9 – Demonstrates step 8: the same doctor and slot cannot be booked twice'),
  P('**Closing confirmation.** Output [5] above shows the window logic: with a booking in progress, the answer NO keeps the window open (confirmClose() = false) and YES closes it; after a successful booking the form is empty, so closing needs no question. **A defect found by the event trace:** in the first version the success message was blank. Resetting the form changes the department combo box, which fires its ItemListener, and that listener cleared the status line right after the message was set. The fix (AppointmentPanel.done(): reset the form first, then show the message) is in the code above; this is the kind of defect that only shows when the order of events is traced.'),
  H2('Alternative concept and complexity analysis'),
  P('**The alternative solution.** Instead of loading all doctors once into a Map<String, List<Doctor>> (the cache used above), the doctor list is fetched from the database every time the department changes:'),
  ...code(`
// cache (used): one query at start-up, then a Map lookup per change
private final Map<String, List<Doctor>> doctorsByDepartment = new TreeMap<>(repository.loadAllDoctors());
public List<Doctor> doctorsFor(String department) { return doctorsByDepartment.getOrDefault(department, List.of()); }

// alternative: one query per change (SELECT d.doctor_id, d.name FROM doctor d JOIN department p ... WHERE p.name = ?)
public List<Doctor> doctorsFor(String department) { return repository.findDoctors(department); }   // must run in a SwingWorker`),
  gap(),
  table(['d departments, m doctors', 'Cached Map', 'Query on every change'], [
    ['Time per department change', 'O(1) lookup in the Map, then O(m/d) to fill the combo box (the same fill is needed in both)', 'O(log m + m/d) with an index on dept_id (O(m) without one) + one network round trip; must run off the event thread'],
    ['Time at start-up', 'one query, O(m)', 'none'],
    ['Memory', 'O(m): all m doctors stay in memory (d lists + m references; the Doctor objects, about 100 bytes each, are the larger part)', 'O(m/d): only the doctors of the current department'],
    ['Freshness', 'stale until reloaded (a new doctor is not seen)', 'always current'],
  ], [2300, 3350, 3376], { leftAll: true, size: 16 }),
  P('**Measurements** (CacheBenchmark.java). The “database” is the in-memory repository, whose query scans all doctors like a query without an index; one real network round trip is assumed to cost 2 ms (an assumption, not a measurement).'),
  table(['d', 'm', 'Map lookup (ns)', 'DB query scan (ns)', 'DB + 2 ms round trip (ms)', 'Map overhead (MB, references only)'],
    c2.map(r => [fmt(r.departments), fmt(r.doctors), f1(r.cache_ns_per_change), fmt(Math.round(r.db_scan_ns_per_change)), f3(r.db_with_2ms_roundtrip_ms), f2(r.cache_extra_MB)]), [700, 1100, 1700, 1900, 2000, 1626], { size: 16 }),
  caption('Table 2.3 – Department change: cache against query (from Task02/cache_results.csv)'),
  ...fig('graphs/task2_cache.png', 420, 300, 'Figure 2.10 – Time per department change (logarithmic axis)'),
  P(`**Analysis.** The cached lookup takes about 0.1–0.4 µs whatever m is (O(1); the first row is affected by warm-up). The query without an index grows with m: ${fmt(Math.round(c2[0].db_scan_ns_per_change / 1000))} µs for 1,000 doctors, about ${fmt(Math.round(c2[1].db_scan_ns_per_change / 1000))} µs for 100,000 and ${f1(c2[3].db_scan_ns_per_change / 1e6)} ms for 200,000 (linear, O(m)); with a database index and a network trip it is a few milliseconds. The Map therefore answers 100 to 10,000 times faster. The Map overhead measured here (${f2(c2[0].cache_extra_MB)}–${f2(c2[3].cache_extra_MB)} MB) counts only the lists and references because the Doctor objects already exist in the simulated database; in a real application the m Doctor objects also stay in memory (an estimate of 100 bytes each gives about 10 MB for 100,000 doctors). **Evaluation:** a hospital has tens or a few hundred doctors that change rarely, so the cache is the better design (the receptionist sees the list instantly); a “Refresh doctors” menu item or a time limit on the cache solves the freshness problem. Fetching on every change is better only for very large or frequently changing data.`),
);

// =====================================================================================================
// TASK 3
// =====================================================================================================
const l3 = csv('Task03/layout_results.csv');
const L3 = n => l3.find(r => r.design === n);
add(
  H1('Task 3 – From Sketch to Layout: Campus Library Search Screen'),
  P('**Bloom\'s level K4 – Analyse. 9 marks.** The screen has a title bar at the top; a search row (text field, category selection and Search button); a results area in the centre; a row of Prev, Next and Reserve buttons at the bottom; and a status message. When the window is resized, **the results area must grow while the search row and the buttons keep their size.** (The category “Choice” of AWT is implemented with its Swing equivalent, JComboBox.)'),
  H2('Task 3.1 – Choose the layout managers'),
  table(['Container', 'Layout manager', 'Justification with respect to the resizing requirement'], [
    ['Screen (root panel)', 'BorderLayout', 'NORTH and SOUTH keep their preferred height; CENTER receives ALL the extra width and height. Putting the results area in CENTER is exactly “results grow, the rest keeps its size”.'],
    ['north panel (title + search row)', 'BorderLayout', 'The title bar goes to NORTH (stretches across the full width, fixed height); the search row goes to CENTER of this small panel. Nesting is needed because the root has only one NORTH slot.'],
    ['searchRow (label, text field, Choice, Search)', 'FlowLayout (LEFT)', 'FlowLayout gives every component its preferred size and never stretches it, so the field and the button keep their size when the window is resized. GridLayout would force equal cell sizes and stretch them, which breaks the requirement.'],
    ['south panel (buttons + status)', 'BorderLayout', 'Button row in CENTER, status message in SOUTH; both keep their preferred height.'],
    ['buttonRow (Prev, Next, Reserve)', 'FlowLayout (CENTER)', 'The three buttons keep their size and stay centred when the width changes.'],
    ['Results area', 'JScrollPane (JTextArea inside)', 'Placed in the BorderLayout CENTER; scroll bars appear when the list is longer than the area.'],
  ], [2300, 1800, 4926], { leftAll: true, size: 16 }),
  P('**CardLayout** was not used: it shows only one component of a container at a time (useful for switching between whole screens, as in Task 6), and this screen must show all its parts together.'),
  H2('Task 3.2 – Nested containers'),
  ...fig('diagrams/containers_task3.png', 570, 640, 'Figure 3.1 – Nested panel structure with the layout manager of each panel (Design A is the chosen design; B and C are the alternatives analysed below)'),
  ...code(`
+--------------------------------------------------------------+  <- LibraryNested (BorderLayout)
| north (BorderLayout)                                          |
|   NORTH : [            Campus Library Search              ]   |
|   CENTER: searchRow (FlowLayout LEFT)                         |
|           Search: [______________] [All v] [Search]           |
+--------------------------------------------------------------+
|                                                              |
|   CENTER: results (JScrollPane)  <- takes all the extra space |
|                                                              |
+--------------------------------------------------------------+
| south (BorderLayout)                                          |
|   CENTER: buttonRow (FlowLayout CENTER)  [Prev] [Next] [Reserve]|
|   SOUTH : status message                                      |
+--------------------------------------------------------------+`),
  gap(),
  H2('Task 3.3 – Implementation'),
  P('The three layout designs share one base class (LibraryScreen) that creates the widgets and the behaviour (search, paging, reserve), so that they differ ONLY in their layout. Design A (LibraryNested) is the chosen solution; B and C are used in the alternative analysis.'),
  ...codeFile('Task03/BookCatalog.java'), ...codeFile('Task03/LibraryScreen.java', 'common widgets and behaviour'),
  ...codeFile('Task03/LibraryNested.java', 'DESIGN A – nested BorderLayout + FlowLayout (chosen)'),
  ...codeFile('Task03/LibraryGridBag.java', 'DESIGN B – GridBagLayout'), ...codeFile('Task03/LibraryAbsolute.java', 'DESIGN C – absolute positioning'),
  ...codeFile('Task03/LibraryFrame.java', 'run with: java LibraryFrame [nested|gridbag|absolute]'),
  H2('Task 3.4 – Test the resizing behaviour'),
  P('The harness (ResizeHarness, Appendix B) lays out each design at three window sizes, prints the size of every region and saves a picture. The printed sizes are the evidence; the pictures show the same thing.'),
  ...outFile('Task03/resize_output.txt', 'Output of ResizeHarness (region sizes in pixels: width x height)'),
  ...fig('screenshots/task03/t3_nested_520x330.png', 330, 260, 'Figure 3.2 – Design A at 520 × 330 (small). Demonstrates the requirement: all regions visible'),
  ...fig('screenshots/task03/t3_nested_800x500.png', 480, 340, 'Figure 3.3 – Design A at 800 × 500 (medium)'),
  ...fig('screenshots/task03/t3_nested_1100x650.png', 560, 400, 'Figure 3.4 – Design A at 1100 × 650 (large)'),
  table(['Region', 'Behaved as designed?', 'Evidence (520×330 → 800×500 → 1100×650)'], [
    ['Title bar', 'Yes: stretches sideways, height fixed', 'width 520 → 800 → 1100, height 23 at all sizes'],
    ['Search row (field, Choice, Search button)', 'Yes: keeps its size', 'text field 224×19 and Search button 75×25 at all three sizes; the row stays at the left'],
    ['Results area', 'Yes: grows in both directions', 'height 204 → 374 → 524 and width 520 → 800 → 1100 (it takes all the extra space)'],
    ['Prev / Next / Reserve buttons', 'Yes: keep their size and stay centred', 'Prev button 61×25 at all three sizes'],
    ['Status message', 'Yes: fixed height, follows the width', 'height 21, width 520 → 800 → 1100'],
  ], [2600, 2600, 3826], { leftAll: true, size: 16 }),
  caption('Table 3.1 – Resizing behaviour of Design A: every region behaved as designed'),
  H2('Alternative concept and complexity analysis'),
  P('**The alternative solutions.** (B) One panel with GridBagLayout: every component gets a cell, and weightx / weighty / fill decide what grows. (C) Absolute positioning: setLayout(null) and setBounds(x, y, w, h) for each of the ten components. The key code is in LibraryGridBag.java and LibraryAbsolute.java above.'),
  ...fig('screenshots/task03/t3_gridbag_800x500.png', 480, 340, 'Figure 3.5 – Design B (GridBagLayout) at 800 × 500: results grow; the search text field also stretches because it was given weightx = 1'),
  ...fig('screenshots/task03/t3_absolute_520x330.png', 330, 260, 'Figure 3.6 – Design C (absolute) at 520 × 330: the window is smaller than the fixed coordinates, so the right side and the bottom (status, Search button) are cut off'),
  ...fig('screenshots/task03/t3_absolute_1100x650.png', 560, 400, 'Figure 3.7 – Design C at 1100 × 650: nothing moved or grew, the results area is still 580 × 240 and the rest is empty'),
  table(['', 'A: nested Border + Flow', 'B: GridBagLayout', 'C: absolute (null layout)'], [
    ['Resizing behaviour', 'As required: results grow, search row and buttons keep their size', 'As required if weights are set correctly; the field can stretch if wanted (more control)', 'None: positions and sizes are fixed numbers; clipped when smaller, empty when larger'],
    ['Layout computation per resize', 'O(c) – a few simple managers, c = number of components', 'O(c) with a larger constant – the manager builds a grid matrix and distributes weights', 'O(c) – only reads the stored bounds (no computation)'],
    ['Container objects (space)', '4 helper JPanels + the root', '1 helper JPanel (buttons) + the root', '0 helper panels; only the root'],
    ['Change effort', 'Easy to read: one manager per panel', 'Powerful but the constraints are hard to read and tune', 'Breaks on other fonts, screen sizes and languages'],
  ], [1900, 2400, 2400, 2326], { leftAll: true, size: 16 }),
  P('**Measurements** (LayoutBenchmark.java: layout of the whole component tree after 20,000 resizes, average of 5 runs):'),
  table(['Design', 'Helper JPanels', 'All containers in the tree', 'Components', 'Layout time (µs per resize)'],
    l3.map(r => [r.design, r.helper_panels, r.all_containers, r.components_total, f2(r.microseconds_per_resize)]), [1800, 1700, 2100, 1500, 1926], { size: 17 }),
  caption('Table 3.2 – Layout cost and number of container objects (from Task03/layout_results.csv)'),
  P(`**Analysis.** All three are linear in the number of components. Absolute positioning is fastest (${f2(L3('absolute').microseconds_per_resize)} µs) because it does no computation, GridBagLayout is slowest (${f2(L3('gridbag').microseconds_per_resize)} µs) because it builds the grid, and the nested design is in between (${f2(L3('nested').microseconds_per_resize)} µs). The nested and GridBag designs need about 0.02–0.03 ms per resize and the absolute design about 0.005 ms; even 0.03 ms is only 0.2% of the 16 ms available for one frame at 60 frames per second, so the speed difference is of no practical importance. The “all containers” column counts the containers inside the Swing components themselves (scroll pane, combo box), which is why it is 21–25 for every design; the number of helper panels we created is 4, 1 and 0. Each JPanel costs only a few hundred bytes, so the extra memory of Design A is negligible. **Evaluation:** Design A is the best choice because it satisfies the resizing requirement with simple managers that are easy to read; GridBagLayout is the alternative when finer control is needed; absolute positioning fails the requirement.`),
);

// =====================================================================================================
// TASK 4
// =====================================================================================================
const r4 = csv('Task04/repaint_results.csv');
add(
  H1('Task 4 – Event-Handling Forensics: The Drawing Pad'),
  P('**Bloom\'s level K4 – Analyse. 7 marks.** A drawing pad should draw a dot wherever the user clicks and show the click count in a label. Users report that nothing is drawn and the count stays at 0. The original program (class renamed DrawPadBuggy so that it can live beside the repaired DrawPad):'),
  ...codeFile('Task04/DrawPadBuggy.java', 'the original program'),
  H2('Task 4.1 – Locate the defect'),
  P('**Why the click is never handled.** The listener class extends MouseAdapter, whose method is called **mouseClicked** (capital C). The program declares **mouseclicked** (small c). Java is case sensitive, so this is a *new, unrelated method*, not an override. When the click happens, AWT calls mouseClicked(MouseEvent), which in MouseAdapter is an empty method, so nothing happens. The compiler does not complain, because declaring an extra method is legal.'),
  P('**Why the label never changes.** Two separate reasons: (1) the method that would change it is never called (above); (2) even if it were called, its body only does count++ and draws a dot; no statement ever calls status.setText(...), so the label would still read “Clicks: 0”. The defect list is therefore: D1 wrong method name; D2 the label is never updated; D3 the dot is drawn with getGraphics() (see 4.2); D4 the click position is used as the top-left corner of the dot, not the centre.'),
  P('**Evidence (real events sent to the real class):**'),
  ...outFile('Task04/drawpad_output.txt', 'Output of DrawPadHarness (section 1 = the original program)'),
  P('Five MOUSE_CLICKED events were dispatched to the window: count = 0, the label is unchanged, and the listener class declares only mouseclicked – it does not override mouseClicked(MouseEvent).'),
  ...fig('screenshots/task04/t4_01_original_after_5_clicks.png', 400, 300, 'Figure 4.1 – Demonstrates Task 4.1: original program after five clicks – no dots, label still “Clicks: 0”'),
  H2('Task 4.2 – Construct a failing interaction'),
  P('**Reproducible steps.** (1) Fix only the spelling (mouseClicked) and add status.setText(...) – this is the class DrawPadStep2 below. (2) Run it and click five times in the window: five dots appear and the label shows “Clicks: 5”. (3) Minimise the window and restore it (or drag another window over it, or resize it). (4) **All dots are gone**, while the label still says “Clicks: 5”.'),
  P('**Why it happens.** getGraphics() returns a graphics context for the *window surface*, so the dot is only pixels on the screen; the program stores nothing. When a window is minimised, covered or resized, the system throws those pixels away. When the window is shown again AWT calls update() and paint(), which redraw the window from the program\'s data. DrawPadStep2 does not override paint(), so the default paint() draws only the components (the label), and the dots are lost. (The label survives because the label component redraws itself.) The rule: **everything that must stay on screen has to be drawn from stored data inside paint().**'),
  ...codeFile('Task04/DrawPadStep2.java', 'defect D1 and D2 repaired, but still drawing with getGraphics()'),
  ...fig('screenshots/task04/t4_02_step2_before_minimise.png', 400, 300, 'Figure 4.2 – Demonstrates Task 4.2 (step 2): five dots, “Clicks: 5”'),
  ...fig('screenshots/task04/t4_03_step2_after_restore.png', 400, 300, 'Figure 4.3 – Demonstrates Task 4.2 (step 4): after minimise + restore (imitated by repainting the window) the dots are gone, the label still says 5'),
  ...fig('diagrams/flow_task4.png', 560, 320, 'Figure 4.4 – Why the dots disappear and how the repaired design keeps them'),
  H2('Task 4.3 – Repair the program (also: Complete the Code, TODO 1 and TODO 2)'),
  P('The points are stored in a List<Point>; the click handler stores the point, updates the label and calls repaint(); paint() draws every stored point as a 6 × 6 filled circle centred on the point (x − 3, y − 3). The two TODOs of the Complete-the-Code program are marked in the code. Nothing else in the given program was changed (a small accessor clickCount() and a call to super.paint(g) were added; super.paint(g) lets AWT draw the label as usual).'),
  ...codeFile('Task04/DrawPad.java', 'TODO 1 and TODO 2 completed'),
  ...outFile('Task04/drawpad_output.txt', 'Output of DrawPadHarness (section 3 = the completed DrawPad)'),
  ...fig('screenshots/task04/t4_04_drawpad_before_minimise.png', 400, 300, 'Figure 4.5 – Complete the Code: DrawPad after five clicks (BEFORE minimising)'),
  ...fig('screenshots/task04/t4_05_drawpad_after_restore.png', 400, 300, 'Figure 4.6 – Complete the Code: the same window AFTER minimise + restore: all five dots are still there (painted again from the stored points)'),
  P('**Note on the screenshots.** On a real desktop, take the two screenshots yourself with the real window (click five times, minimise, restore). The two pictures above come from the headless test harness: it sends five MouseEvents to the real DrawPad object and asks it to paint itself, once before and once after the imitated “minimise/restore”.'),
  H2('Task 4.4 – Prevent recurrence with @Override'),
  P('The experiment from the Complete-the-Code section was performed with the real compiler. (1) With **@Override removed** and the method misspelled as mouseclicked, the program compiles without any message – the bug is invisible. (2) With **@Override** written above the misspelled method, javac stops with an error. The compiler output:'),
  ...outFile('Task04/compile_no_override_output.txt', '(1) javac DrawPadNoOverride.java   (no @Override, misspelled)'),
  ...outFile('Task04/compile_error_output.txt', '(2) javac DrawPadWithOverride.java   (@Override, misspelled)'),
  P('@Override tells the compiler “this method is meant to replace a method of the superclass or interface”. If no such method exists with that name and parameter list, the compiler reports “method does not override or implement a method from a supertype”. The original defect would therefore have been found at compile time, in the first second, instead of by users who see nothing happen. The same applies to wrong parameter types (for example mouseClicked(ActionEvent)). Using an IDE that adds @Override automatically, or a MouseAdapter lambda-free listener with @Override on every handler, removes this whole class of mistakes.'),
  ...codeFile('Task04/errors/DrawPadNoOverride.java', 'experiment (1)'),
  ...codeFile('Task04/errors/DrawPadWithOverride.java', 'experiment (2)'),
  H2('Alternative concept and complexity analysis'),
  P('**The alternative solution.** Instead of storing the points and redrawing all of them in paint(), draw each dot **once** onto an off-screen BufferedImage and let paint() copy that image to the window with one drawImage call:'),
  ...code(`
// repaired design (stored points)                      // alternative (off-screen image)
points.add(e.getPoint());                               Graphics2D g2 = canvas.createGraphics();
repaint();                                              g2.fillOval(e.getX() - 3, e.getY() - 3, 6, 6); g2.dispose();
paint(g): for (Point p : points)                        paint(g): g.drawImage(canvas, 0, 0, null);   // one copy
              g.fillOval(p.x - 3, p.y - 3, 6, 6);`),
  gap(),
  ...codeFile('Task04/DrawPadBuffered.java', 'the alternative'),
  table(['', 'Stored points + paint()', 'Off-screen BufferedImage'], [
    ['Time per click', 'O(1) (add a point) + one repaint', 'O(1) (draw one dot) + one repaint'],
    ['Time per repaint (n clicks)', 'O(n): every point is drawn again', 'O(W × H): one image copy, independent of n'],
    ['Space (n clicks)', 'O(n): about 40 bytes per point', 'O(W × H): 4 bytes per pixel (400 × 300 = 480,000 bytes; 1920 × 1080 = 8.3 MB), independent of n'],
    ['Other properties', 'Data survive: can undo, save, redraw at another size', 'Pixels only: no undo, size fixed (must be enlarged when the window grows)'],
  ], [2300, 3400, 3326], { leftAll: true, size: 16 }),
  P('**Measurements** (RepaintBenchmark.java; window 400 × 300 painted into an off-screen surface):'),
  table(['n clicks', 'Redraw n points (µs)', 'Copy one image (µs)', 'Points memory (bytes)', 'Image memory (bytes)'],
    r4.map(r => [fmt(r.n), f1(r.points_repaint_us), f1(r.image_repaint_us), fmt(r.points_memory_bytes), fmt(r.image_memory_bytes)]), [1300, 2000, 1900, 1900, 1926], { size: 16 }),
  caption('Table 4.1 – Repaint time and memory (from Task04/repaint_results.csv)'),
  ...fig('graphs/task4_repaint.png', 420, 300, 'Figure 4.7 – Time per repaint against the number of clicks (logarithmic axes)'),
  P(`**Analysis.** Redrawing the points costs about 1 µs per point: ${f1(r4[1].points_repaint_us)} µs for 1,000 points, ${f1(r4[3].points_repaint_us / 1000)} ms for 100,000 and ${f1(r4[4].points_repaint_us / 1000)} ms (almost a second) for 1,000,000 – linear, O(n). Copying the image costs about 0.1–0.3 ms whatever n is (O(W × H); the small differences are noise). With about 1 µs per point against 0.1–0.3 ms per image copy, the two curves cross somewhere between 100 and 300 points (the n = 100 row is noisy), after which the image is faster; at 100,000 clicks it is about 900 times faster. For memory the points need about ${Math.round(r4[4].points_memory_bytes / r4[4].n)} bytes each (${fmt(r4[4].points_memory_bytes)} bytes for 1,000,000), the image always ${fmt(r4[0].image_memory_bytes)} bytes for a 400 × 300 window; the break-even is about 13,000 points for 400 × 300, and about 225,000 points for a 1920 × 1080 image (8.3 MB). **Evaluation:** for a drawing pad where a user makes some hundreds of clicks, the stored points are simpler and keep the data (undo, save); for thousands of strokes or freehand drawing the off-screen image is the better design. (These are software-rendered times on an off-screen surface; a real window adds the cost of copying to the screen in both cases.)`),
);

// =====================================================================================================
// TASK 5
// =====================================================================================================
add(
  H1('Task 5 – Same Screen, Different Event-Handling Styles: Currency Converter'),
  P('**Bloom\'s level K5 – Evaluate. 7 marks.** A currency converter has an amount field, a currency selection, a Convert button and a result label (INR to USD or EUR; rates 0.012 and 0.011). The result is shown with two decimals, or “Invalid amount” when the text is not a number.'),
  H2('Task 5.1 – Four implementations'),
  P('All four versions have the same window, the same convert() method and the same window-closing code (a WindowAdapter). They differ only in how the Convert button\'s listener is written.'),
  ...codeFile('Task05/ConverterA.java', 'style (a): the frame itself implements ActionListener'),
  ...codeFile('Task05/ConverterB.java', 'style (b): named inner class'),
  ...codeFile('Task05/ConverterC.java', 'style (c): anonymous inner class'),
  ...codeFile('Task05/ConverterD.java', 'style (d): lambda expression'),
  P('**Results (from the harness; amount 5000 INR to EUR, and the text “abc”).** The first four rows of the table are these versions:'),
  ...outFile('Task05/converter_output.txt', 'Output of ConverterHarness (rows ConverterA … ConverterD = Task 5.1)'),
  H2('Task 5.2 – Change the requirement (Enter key + result dialog)'),
  P('The converter must now also convert when the user presses Enter in the amount field, and show the result in a dialog box. In the output table above, the rows ConverterA … D (before) show “(nothing)” for the Enter key and “no” dialog; the rows ConverterA2 … D2 (after) convert on Enter and show the dialog (“dialog text 55.00 EUR”).'),
  P('**What had to change in each version** (measured with a line diff between the base file and the modified file):'),
  ...outFile('Task05/change_effort.txt', 'Task05/change_effort.txt'),
  table(['Style', 'Changes needed for Enter key', 'Changes needed for the dialog', 'Total lines changed'], [
    ['(a) frame implements ActionListener', '1 line: amount.addActionListener(this)', '2 lines in convert() (result and error)', '3 (3 added, 0 removed)'],
    ['(b) named inner class', '1 line: amount.addActionListener(new ConvertListener())', '2 lines in convert()', '3 (3 added, 0 removed)'],
    ['(c) anonymous inner class', 'The anonymous object cannot be reused as written: it had to be stored in a variable first, then registered twice (the registration code was rewritten)', '2 lines in convert()', '8 (6 added, 2 removed)'],
    ['(d) lambda', 'Store the lambda in a variable, register it twice (a lambda also has no name)', '2 lines in convert()', '6 (5 added, 1 removed)'],
  ], [2300, 3300, 1900, 1526], { leftAll: true, size: 16 }),
  caption('Table 5.1 – Change effort (the “lines” numbers are from the diff above)'),
  H3('The modified versions'),
  ...codeFile('Task05/ConverterA2.java'), ...codeFile('Task05/ConverterB2.java'), ...codeFile('Task05/ConverterC2.java'), ...codeFile('Task05/ConverterD2.java'),
  H2('Task 5.3 – Compare the styles'),
  table(['Criterion', '(a) frame implements', '(b) named inner class', '(c) anonymous class', '(d) lambda'], [
    ['Readability', 'The listener code is far from the button (the whole frame is the listener); with many buttons actionPerformed becomes an if-else chain', 'Handler has a name and a place; more lines (boilerplate)', 'Handler is next to the button that uses it; verbose (6 lines to call one method)', 'Shortest; handler next to the button; best when the body is one call'],
    ['Access to the frame\'s fields', 'Direct (it is the frame)', 'Direct: an inner class can use the outer fields', 'Direct (captures the frame)', 'Direct (captures this)'],
    ['Reuse of the handler', 'One handler for all sources: need e.getSource() or the command to tell them apart', 'Best: a named class can be instantiated for several components or reused', 'Poor: no name; must be kept in a variable to be reused', 'Poor-fair: keep it in a variable (an ActionListener) or call a shared method'],
    ['Ease of change', 'Every new trigger changes the shared method', 'Add a registration; add a class for a new behaviour', 'Duplicates code or needs refactoring (measured: 8 lines for the changed requirement)', 'Small edits; refactoring needed for reuse (6 lines)'],
  ], [1500, 1900, 1900, 1900, 1826], { leftAll: true, size: 15 }),
  caption('Table 5.2 – Comparison of the four styles'),
  P('**Which style for a large application?** I would not let the frame implement the listener (a), because with 30 buttons one actionPerformed() becomes a long if-else chain (as in Task 1). For a large application I would use **small lambdas or method references that only call a method of a controller/service class** for one-trigger actions (readable, no boilerplate, handler next to the component), and **a shared Action object** (alternative f below) for any command that has several triggers (button, menu item, keyboard shortcut). Named inner classes are justified when a handler is big, has its own state or is reused. This keeps the interface separate from the logic, which is the main lesson of the assignment.'),
  H2('Complete the Code – Converter.java (style d)'),
  P('TODO 1 registers **one** lambda for both the Convert button and the Enter key of the amount field (the same listener object is attached to both: the harness reports “same object? true”). TODO 2 parses the amount and shows the value with two decimals, or “Invalid amount”.'),
  ...codeFile('Task05/Converter.java', 'TODO 1 and TODO 2 completed'),
  ...fig('screenshots/task05/t5_01_valid_conversion.png', 420, 160, 'Figure 5.1 – Complete the Code: a valid conversion (2500 INR → 30.00 USD, Convert button clicked)'),
  ...fig('screenshots/task05/t5_02_invalid_amount.png', 420, 160, 'Figure 5.2 – Complete the Code: an invalid amount (“12abc”) shows “Invalid amount”'),
  P('Results printed by the harness for this program: 2500 INR → 30.00 USD (button); 5000 INR → 55.00 EUR (Enter key); “12abc” → Invalid amount; empty field → Invalid amount.'),
  H2('Alternative concept and complexity analysis'),
  P('**The alternative solutions.** (e) A **method reference**: convertButton.addActionListener(this::onConvert), where onConvert(ActionEvent e) is an ordinary method. (f) **One shared Action object** (AbstractAction) holding the name, the keyboard shortcut and the code, used by the button, a menu item and the Enter key:'),
  ...code(`
// (e) method reference
convertButton.addActionListener(this::onConvert);
amount.addActionListener(this::onConvert);

// (f) one shared Action for three triggers
Action convertAction = new ConvertAction();             // name "Convert", accelerator Ctrl+Enter, the code
convertButton.setAction(convertAction);                 // trigger 1: button
amount.addActionListener(convertAction);                // trigger 2: Enter in the field
menu.add(new JMenuItem(convertAction));                 // trigger 3: menu item (shows Ctrl+Enter)`),
  gap(),
  ...codeFile('Task05/ConverterE.java', 'alternative (e): method reference'), ...codeFile('Task05/ConverterF.java', 'alternative (f): shared Action, menu item, shortcut'),
  P('The harness output (section “Listener objects registered per version”) gives the number of listener objects. Combined with the change-effort table, the comparison is:'),
  table(['Version (with Enter key + dialog)', 'Registrations on triggers', 'Distinct listener objects', 'Work to add one more trigger (for example a menu item)'], [
    ['(a) frame implements', '2', '1 (the frame itself)', '1 line: item.addActionListener(this)'],
    ['(b) named inner class', '2', '2 (new ConvertListener() each time) – or 1 if the instance is stored', '1 line + a new object (or store the instance)'],
    ['(c) anonymous class', '2', '1 (after storing it in a variable)', '1 line, but only after the refactoring to a variable'],
    ['(d) lambda', '2', '1 (after storing it in a variable)', '1 line, after the same refactoring'],
    ['(e) method reference', '2', '2 (a method reference is evaluated, so a new object, for every registration)', '1 line: item.addActionListener(this::onConvert)'],
    ['(f) shared Action', '3 (button, field, menu item)', '1 (the Action is shared)', '1 line: new JMenuItem(action) – the name, shortcut and code come with it'],
  ], [2400, 1700, 2600, 2326], { leftAll: true, size: 16 }),
  caption('Table 5.3 – Listener objects (counted by the harness) and effort for a new trigger'),
  P('**Analysis.** Space: a listener object is small (about 16–24 bytes), so the difference between one and two objects is irrelevant for memory; the Action version also keeps a few property-change registrations (a button that uses an Action listens to changes of the Action\'s text and enabled state), but these are also tiny. Time to dispatch: O(1) in every style. The real difference is **work and risk when requirements change**: the shared Action is the only version where a new trigger needs a single line and automatically gets the same name, shortcut and enabled/disabled state (disabling the Action disables the button and the menu item together). That is why it is the best choice when one command has several triggers.'),
);

// =====================================================================================================
// TASK 6
// =====================================================================================================
const pg = csv('Task06/pagination_results.csv');
const sqlOut = rd('Task06/sql_logic_check_output.txt');
const msOf = label => { const m = sqlOut.match(new RegExp(label.replace(/[()]/g, '\\$&') + '[^\\n]*?([0-9.]+) ms')); return m ? m[1] : '?'; };
const sqlAll = msOf('all rows (no LIMIT)'), sqlOff0 = msOf('page of 50, OFFSET 0'), sqlOff50 = msOf('page of 50, OFFSET 50,000'), sqlOffLast = msOf('page of 50, OFFSET 99,950 (last page)');
const PG = (n, a) => pg.find(r => Number(r.rows) === n && r.approach === a);

add(
  H1('Task 6 – Campus Event Registration Desktop Application (Swing + JDBC + MySQL)'),
  P('**Bloom\'s level K5 – Evaluate. 13 marks.** The cultural committee needs a desktop application in which students register for campus events and the committee views, filters and exports the registrations. The application is built in the progression required by the assignment: Interpret the requirement → Identify components and events → Design the layout → Design the handlers → Implement → Trace → Challenge → Refine.'),
  H2('Stage 1 – Interpret the Problem (2 marks)'),
  P('**Users:** (1) a **student** (or the committee desk operator on the student\'s behalf) who fills the registration form; (2) a **committee member** who looks at the registrations, filters them by event and exports them. **Data:** students (register number, name, e-mail), events (title, venue, date) and registrations (which student registered for which event, and when).'),
  P('Each requirement below is a **testable statement**; the Test column says where in this report it is demonstrated (the final figure captions refer to these identifiers).'),
  table(['ID', 'Testable requirement', 'Demonstrated by'], [
    ['REQ-01', 'The register number must be exactly 9 digits; anything else is rejected with a message.', 'Test T1 – ServiceDemo “register number too short / with letters”'],
    ['REQ-02', 'The name may contain only letters, spaces and dots (2–60 characters).', 'T2 – “name with digits”, “SQL text in the name”'],
    ['REQ-03', 'The e-mail address must have a valid form.', 'T3 – “e-mail without @”; Figure 6.8'],
    ['REQ-04', 'All fields are required and an event must be selected.', 'T4 – “all fields empty”, “no event selected”; Figure 6.7'],
    ['REQ-05', 'A student can register only once for the same event (duplicate is rejected, nothing is stored twice).', 'T5 – “same student, same event again”, direct insert blocked by UNIQUE key; SQL check; Figure 6.10'],
    ['REQ-06', 'The same student can register for different events.', 'T6 – “same student, another event”; SQL check'],
    ['REQ-07', 'Registrations are stored in a MySQL database through JDBC; the student row and the registration row are saved together or not at all.', 'Code: JdbcRegistrationDao (PreparedStatement, transaction); SQL script; SQL check (SQLite stand-in)'],
    ['REQ-08', 'All registrations are shown in a table, one page at a time, and can be filtered by event.', 'T8 – Figures 6.11 to 6.13'],
    ['REQ-09', 'The selection can be exported to a CSV file with the same rows.', 'T9 – ServiceDemo “Export”'],
    ['REQ-10', 'A menu bar offers File (Export, Exit), View (screens, refresh) and Help (About).', 'T10 – menu output and Figure 6.6'],
    ['REQ-11', 'The window never freezes while the database is working.', 'T11 – responsiveness measurement (slowest answer 2.00 ms during an 855 ms load)'],
    ['REQ-12', 'Closing the window while work is running asks for confirmation.', 'Code: MainFrame.requestExit() (same pattern as Tasks 1 and 2)'],
  ], [900, 5100, 3026], { leftAll: true, size: 16 }),
  caption('Table 6.1 – Requirements and tests'),
  H2('Stage 2 – Propose the Screen and Event Flow (2 marks)'),
  P('**Screen flow.** The application has two screens inside one window (a CardLayout) and three dialogs.'),
  ...fig('diagrams/flow_task6.png', 560, 300, 'Figure 6.1 – Screen flow and menu structure'),
  ...code(`
Screen 1 - Register                                   Screen 2 - Registrations
+---------------------------------------------+      +---------------------------------------------+
| File   View   Help                          |      | File   View   Help                          |
+---------------------------------------------+      +---------------------------------------------+
| Register for an event                       |      | Show event: [All events v]  [Refresh]       |
| Register number: [______________]           |      | +-----+----------+---------+-------+------+|
| Student name:    [______________]           |      | |RegID|Register N|Student  |Event  |Date  ||
| E-mail:          [______________]           |      | |  1  |192311001 |Arun K.  |Tech.. |...   ||
| Event:           [-- select -- v]           |      | |  2  |192311002 |Divya S  |Cultu..|...   ||
|        [ Register ]   [ Clear ]             |      | +-----+----------+---------+-------+------+|
| status message (green = ok, red = error)    |      |      [Prev]  Page 1 of 3  [Next]            |
+---------------------------------------------+      | 13 registration(s) found                    |
                                                     +---------------------------------------------+
Menu:  File > Export CSV... (Ctrl+E), Exit | View > Register screen, Registrations screen, Refresh table | Help > About`),
  gap(),
  table(['Component', 'Swing class', 'Event → listener → action'], [
    ['Register number, name, e-mail', 'JTextField', 'typed text is read when Register is clicked'],
    ['Event', 'JComboBox<CampusEvent>', 'item chosen is read when Register is clicked'],
    ['Register / Clear', 'JButton', 'ActionEvent → ActionListener (lambda) → register() in a SwingWorker / clear()'],
    ['Filter by event', 'JComboBox', 'ActionEvent → load(1) in a SwingWorker'],
    ['Prev / Next / Refresh', 'JButton', 'ActionEvent → load(page − 1 / page + 1 / page)'],
    ['Registrations table', 'JTable + DefaultTableModel (not editable) in a JScrollPane', 'filled in done() of the SwingWorker (event-dispatch thread)'],
    ['Menu bar', 'JMenuBar, JMenu, JMenuItem (with Ctrl+E accelerator)', 'ActionEvent → showScreen / exportCsv / requestExit / About dialog'],
    ['Export, About, Exit dialogs', 'JFileChooser, JOptionPane', 'modal dialogs started from menu actions'],
    ['Main window', 'JFrame + WindowAdapter', 'windowClosing → requestExit()'],
  ], [2700, 2700, 3626], { leftAll: true, size: 16 }),
  H2('Stage 3 – Identify the Critical Design Decisions (2 marks)'),
  table(['Decision', 'What I decided', 'Why (alternative rejected)'], [
    ['Where is input validated?', 'In RegistrationService (one place). The window only passes the text and shows the message.', 'Validation in the listener cannot be tested without a window and is repeated for every screen.'],
    ['Where is “already registered” enforced?', 'Twice: the service asks the DAO first (friendly message), and the database has UNIQUE (reg_no, event_id) as the final guard; a violation (MySQL error 1062) becomes DuplicateRegistrationException.', 'The check alone is not safe when two desks register the same student at the same moment (a race between “check” and “insert”); only the database constraint is.'],
    ['Where does the SQL live?', 'Only in JdbcRegistrationDao, behind the RegistrationDao interface.', 'SQL in listeners ties the window to MySQL and cannot be tested; the interface also allows the in-memory DAO used for tests.'],
    ['How is the database accessed?', 'PreparedStatement for every statement; try-with-resources closes ResultSet, Statement and Connection; a transaction saves student + registration together (commit / rollback).', 'Concatenated SQL allows injection; unclosed connections leak (Task 1, D6 and D7).'],
    ['Which thread does the database work?', 'A SwingWorker: doInBackground() calls the service; done() (event-dispatch thread) updates the window.', 'JDBC on the event thread freezes the window (Task 1, D5).'],
    ['How are errors shown?', 'Expected problems (invalid input, duplicate) come back as a ServiceResult message; unexpected database problems are caught as DataAccessException and shown as “Database problem: …”.', 'A stack trace on the console is invisible to the user.'],
    ['How is a big table shown?', 'Paging: SQL LIMIT/OFFSET, one page (10 rows; 5 in the demo) at a time, plus Prev/Next.', 'Loading all rows blocks the window and uses memory proportional to the table size (measured below).'],
    ['How are the two screens switched?', 'CardLayout in one JFrame, menu items View > Register / Registrations.', 'Two windows would need more window management; the screens are used one at a time.'],
  ], [2200, 3800, 3026], { leftAll: true, size: 16 }),
  caption('Table 6.2 – Design decisions'),
  ...fig('diagrams/arch_task6.png', 500, 520, 'Figure 6.2 – Layered architecture: ui → service → dao → MySQL'),
  ...fig('diagrams/threads_task6.png', 560, 220, 'Figure 6.3 – Which thread does what'),
  H2('Stage 4 – Design the Classes and Handlers (4 marks)'),
  ...fig('diagrams/class_task6.png', 570, 640, 'Figure 6.4 – Class diagram (user-interface, service and data-access classes)'),
  ...code(`
ALGORITHM RegisterButtonClicked                      // runs on the event-dispatch thread
INPUT   : text of regNo, name, email fields; selected event
OUTPUT  : status message; table refreshed when the registration succeeded
BEGIN
  1.  SET regNo, name, email, event <- values currently shown in the form
  2.  CALL registerButton.setEnabled(false)
  3.  CALL status.setText("Saving ...")
  4.  START a SwingWorker whose background step is:                  // worker thread, not the event thread
  5.      RETURN service.register(regNo, name, email, event)
  6.  WHEN the worker is done (back on the event-dispatch thread) DO
  7.      SET result <- worker result
  8.      CALL status.setText(result.message) in green if result.success, otherwise in red
  9.      IF result.success THEN  CALL clearFields()  AND  CALL listPanel.load(1)  ENDIF
  10.     CALL registerButton.setEnabled(true)
  11. ENDWHEN
END

ALGORITHM Register (service)                         // worker thread
INPUT   : regNo, name, email, event
OUTPUT  : ServiceResult
BEGIN
  1.  IF any of regNo, name, email is empty THEN RETURN failure "all fields required" ENDIF
  2.  IF regNo is not 9 digits THEN RETURN failure ENDIF
  3.  IF name does not match letters / spaces / dots THEN RETURN failure ENDIF
  4.  IF email does not match the e-mail pattern THEN RETURN failure ENDIF
  5.  IF event is not selected THEN RETURN failure ENDIF
  6.  IF dao.exists(regNo, event) THEN RETURN failure "already registered" ENDIF
  7.  TRY  CALL dao.register(student, event)                         // transaction, UNIQUE key as last guard
  8.  CATCH DuplicateRegistrationException  RETURN failure "already registered"
  9.  CATCH DataAccessException             RETURN failure "Database problem: ..."
  10. RETURN success "Registered name for event"
END

ALGORITHM LoadPage                                   // filter changed, Prev, Next or Refresh
INPUT   : filter (event or All), requested page number, page size
OUTPUT  : table shows one page; Prev/Next enabled according to the page
BEGIN
  1.  CALL setControlsEnabled(false)  AND  CALL status.setText("Loading ...")
  2.  START a SwingWorker whose background step is:
  3.      SET total <- dao.count(filter)
  4.      SET pages <- max(1, ceiling(total / pageSize))
  5.      SET page  <- limit the requested page number to the range 1 .. pages
  6.      SET rows  <- dao.page(filter, pageSize, (page - 1) * pageSize)       // SQL LIMIT pageSize OFFSET (page-1)*pageSize
  7.      RETURN (rows, total, page, pages)
  8.  WHEN done DO
  9.      CALL table model: remove all rows, then add the returned rows
  10.     CALL pageLabel.setText("Page page of pages")  AND  status.setText(total + " registration(s) found")
  11.     CALL setControlsEnabled(true); Prev is disabled on page 1, Next on the last page
  12. ENDWHEN
END`),
  gap(),
  H2('Stage 5 – Implement and Demonstrate (3 marks): the application'),
  P('The application is a Java package campus.reg with four sub-packages (model, dao, service, ui). It compiles with a plain JDK; MySQL Connector/J is needed only to run the JDBC version.'),
  ...codeFile('Task06/sql/campus_events.sql', 'MySQL script: tables, constraints, sample data'),
  ...codeFile('Task06/src/campus/reg/model/CampusEvent.java'), ...codeFile('Task06/src/campus/reg/model/Student.java'),
  ...codeFile('Task06/src/campus/reg/model/RegistrationRow.java'), ...codeFile('Task06/src/campus/reg/model/PageData.java'),
  ...codeFile('Task06/src/campus/reg/dao/DataAccessException.java'), ...codeFile('Task06/src/campus/reg/dao/DuplicateRegistrationException.java'),
  ...codeFile('Task06/src/campus/reg/dao/RegistrationDao.java'), ...codeFile('Task06/src/campus/reg/dao/JdbcRegistrationDao.java', 'MySQL / JDBC'),
  ...codeFile('Task06/src/campus/reg/dao/InMemoryRegistrationDao.java', 'demo, tests, benchmarks'),
  ...codeFile('Task06/src/campus/reg/service/ServiceResult.java'), ...codeFile('Task06/src/campus/reg/service/RegistrationService.java'),
  ...codeFile('Task06/src/campus/reg/ui/RegisterPanel.java'), ...codeFile('Task06/src/campus/reg/ui/RegistrationsPanel.java'),
  ...codeFile('Task06/src/campus/reg/ui/MainFrame.java', 'JMenuBar, CardLayout, export, exit'),
  ...codeFile('Task06/src/campus/reg/App.java'), ...codeFile('Task06/test/ServiceDemo.java', 'console demonstration of normal, invalid and duplicate cases'),
  H3('Database design (ER diagram)'),
  ...fig('diagrams/er_task6.png', 570, 330, 'Figure 6.5 – ER diagram: a student can register for many events and an event has many students; REGISTRATION resolves the many-to-many relationship and carries the UNIQUE key'),
  H2('Stage 5 (continued) – Demonstration of normal, invalid and duplicate registrations'),
  P('**Normal, invalid and duplicate cases at service level (output of java ServiceDemo):**'),
  ...outFile('Task06/service_demo_output.txt', 'Output of ServiceDemo (tests T1–T9)'),
  P('**The same cases through the real window** (the real MainFrame, RegisterPanel and RegistrationsPanel with their SwingWorkers, driven by button clicks; off-screen render). The output lists the menu structure, each step and the responsiveness measurement:'),
  ...outFile('Task06/registration_ui_output.txt', 'Output of RegistrationHarness'),
  ...fig('screenshots/task06/t6_01_register_screen.png', 460, 340, 'Figure 6.6 – Demonstrates REQ-10: the Register screen with the menu bar (File, View, Help)'),
  ...fig('screenshots/task06/t6_02_invalid_empty.png', 460, 340, 'Figure 6.7 – Demonstrates REQ-04 (T4): empty form'),
  ...fig('screenshots/task06/t6_03_invalid_email.png', 460, 340, 'Figure 6.8 – Demonstrates REQ-03 (T3): invalid e-mail address'),
  ...fig('screenshots/task06/t6_04_normal_registration.png', 460, 340, 'Figure 6.9 – Demonstrates the normal case (REQ-07): registration saved'),
  ...fig('screenshots/task06/t6_05_duplicate.png', 460, 340, 'Figure 6.10 – Demonstrates REQ-05 (T5): the same student and event again is rejected'),
  ...fig('screenshots/task06/t6_06_registrations_page1.png', 460, 340, 'Figure 6.11 – Demonstrates REQ-08 (T8): Registrations screen, page 1 of 3 (13 registrations, 5 per page in the demo)'),
  ...fig('screenshots/task06/t6_07_registrations_page2.png', 460, 340, 'Figure 6.12 – Demonstrates REQ-08: the Next button shows page 2'),
  ...fig('screenshots/task06/t6_08_filter_cultural_night.png', 460, 340, 'Figure 6.13 – Demonstrates REQ-08: filtered by the event “Cultural Night” (4 registrations)'),
  P('**REQ-11 (the window never freezes).** With a database that needs 400 ms per call (count + page = 2 calls, 855 ms in total) the Registrations screen showed “Loading ...” while the event-dispatch thread answered 21 test requests, the slowest in 2.00 ms. The window therefore stayed responsive for the whole load.'),
  H3('SQL script check (SQLite stand-in)'),
  P('No MySQL server was available while preparing this report, so the logic of the MySQL script (constraints, joins, paging) was checked with SQLite after a mechanical conversion (sql_logic_check.py). SQLite is not MySQL: the error texts and the upsert syntax differ, so this verifies the table design and the queries, not the MySQL driver. Run campus_events.sql in MySQL and the JDBC version of the application on your computer for the final submission.'),
  ...outFile('Task06/sql_logic_check_output.txt', 'Output of python3 sql_logic_check.py'),
  H2('Alternative Concept and Complexity Analysis'),
  P('**The alternative solution.** Instead of one page at a time (SQL LIMIT/OFFSET), load **all** registrations at once and let JTable scroll:'),
  ...code(`
// used: one page of 50 rows                                    // alternative: everything at once
rows = dao.page(eventId, 50, (page - 1) * 50);                  rows = dao.findAll(eventId);       // SELECT ... without LIMIT
model.setRowCount(0);                                           model.setRowCount(0);
for (RegistrationRow r : rows) model.addRow(...);               for (RegistrationRow r : rows) model.addRow(...);   // N rows`),
  gap(),
  table(['N registrations, page size P', 'One page (used)', 'All rows at once'], [
    ['Fetch time', 'O(P) rows transferred, plus the cost of skipping OFFSET rows in the database', 'O(N) rows transferred'],
    ['Work on the event-dispatch thread (fill the table model)', 'O(P)', 'O(N): the window is blocked while the model is filled, even if the fetch ran in a worker'],
    ['Memory in the application', 'O(P)', 'O(N)'],
    ['Search / sort across all rows in the window', 'needs a new query', 'free, but only while the data fit in memory'],
  ], [3000, 3000, 3026], { leftAll: true, size: 16 }),
  P('**Measurements** (PaginationBenchmark.java; the “database” is an in-memory DAO without network time, so the fetch times are lower than MySQL would give; the table-model work on the event-dispatch thread and its memory are real; average of 5 runs):'),
  table(['Rows N', 'Approach', 'Fetch (ms)', 'Event thread blocked: fill model (ms)', 'Event-thread allocation (MB)'],
    pg.map(r => [fmt(r.rows), r.approach, f2(r.fetch_ms), f2(r.edt_model_fill_ms), f2(r.edt_allocated_MB)]), [1300, 1800, 1500, 2600, 1826], { size: 16 }),
  caption('Table 6.3 – Everything at once against one page of 50 (from Task06/pagination_results.csv)'),
  ...fig('graphs/task6_pagination.png', 420, 300, 'Figure 6.14 – Time the event-dispatch thread is blocked while the table is filled (logarithmic axes)'),
  P(`**Analysis.** Filling the table with 100,000 rows blocks the event-dispatch thread for ${f1(PG(100000, 'all at once').edt_model_fill_ms)} ms and allocates ${f1(PG(100000, 'all at once').edt_allocated_MB)} MB, against ${f2(PG(100000, 'page of 50').edt_model_fill_ms)} ms and ${f2(PG(100000, 'page of 50').edt_allocated_MB)} MB for one page: the all-at-once cost grows linearly with N (1,000 → 10,000 → 100,000 rows: ${f2(PG(1000, 'all at once').edt_model_fill_ms)} → ${f2(PG(10000, 'all at once').edt_model_fill_ms)} → ${f2(PG(100000, 'all at once').edt_model_fill_ms)} ms), the page cost does not depend on N. With a real MySQL server the fetch of 100,000 rows would add network and driver time (not measured here), which only strengthens the conclusion. **Cost of the paging itself:** LIMIT/OFFSET makes the database skip the first OFFSET rows, so deep pages are slower. In the SQLite check, a page of 50 took ${sqlOff0} ms at OFFSET 0, ${sqlOff50} ms at OFFSET 50,000 and ${sqlOffLast} ms at OFFSET 99,950, while loading all 100,000 rows took ${sqlAll} ms (O(offset + P) for a page against O(N) for everything). If the committee ever has millions of registrations, the refinement is keyset paging (WHERE reg_id > ? ORDER BY reg_id LIMIT 50), which is O(P) at any depth. **Effect on the responsiveness of the window.** *One page in a SwingWorker (used):* the fetch runs on the worker thread and the event thread only fills 50 rows (about 0.02 ms), so the window answered every request within 2 ms while a slow database was being read (REQ-11). *All rows at once:* even if the fetch is moved to a worker, the table model of 100,000 rows must be filled on the event thread, which blocks it for the measured time above (about two frames of animation at 60 frames per second for this in-memory stand-in; with a real JTable rendering and a real database the pause is longer); and if the fetch is done directly in the button\'s listener, the window freezes for the whole fetch plus the fill (hundreds of milliseconds up to seconds with MySQL; not measured here). **Evaluation:** paging is the right design for this application; loading everything is acceptable only for a few hundred rows.`),
  H3('Refinements made after tracing'),
  B('A status message was wiped by a listener (found in Task 2 by tracing events); the same “reset first, message second” order is used in RegisterPanel.'),
  B('The Exit menu item first built a synthetic WindowEvent; it was replaced by one method requestExit() shared by the menu item and the window-closing handler (shorter, testable).'),
  B('The table header did not appear in the off-screen render because JTable installs it only when the window is shown; the header is now set explicitly (scroll.setColumnHeaderView) and the columns have sensible widths.'),
  H3('Known limitations (honest list)'),
  B('The JDBC classes and the MySQL script were not executed against a MySQL server in the preparation of this report (see the SQLite check); they must be run once on your computer.'),
  B('Student rows are updated when the same register number is used with a new name or e-mail (ON DUPLICATE KEY UPDATE); a real system would also authenticate the committee desk.'),
  B('The export writes the whole selection in the worker thread but has no progress bar.'),
);

// =====================================================================================================
// APPENDICES
// =====================================================================================================
add(
  H1('Appendix A – Repository Layout, How to Run, GitHub Steps'),
  ...code(`
CSA09-<RegisterNumber>-Assignment4/
  README.md   GITHUB_STEPS.md
  Task01/  FeeService, FeePanel, FeeWindow, repositories, tests, schema.sql, outputs, dispatch_results.csv
  Task02/  AppointmentService, AppointmentPanel, AppointmentWindow, WindowTrace, schema.sql, outputs, cache_results.csv
  Task03/  LibraryScreen, LibraryNested, LibraryGridBag, LibraryAbsolute, LibraryFrame, outputs, layout_results.csv
  Task04/  DrawPadBuggy, DrawPadStep2, DrawPad, DrawPadBuffered, errors/, compile outputs, repaint_results.csv
  Task05/  ConverterA..D, ConverterA2..D2, ConverterE, ConverterF, Converter (Complete the Code), outputs, change_effort.txt
  Task06/  src/campus/reg/{model,dao,service,ui}, sql/campus_events.sql, sql/sql_logic_check.py, test/, outputs
  diagrams/    ER diagrams, class / container / flow diagrams (Graphviz .dot + .png)
  screenshots/ one folder per task (off-screen renders; replace by real screenshots)
  graphs/      figures of the measurements
  tools/       headless shims, harness, run_headless.sh
  report/      this report`),
  gap(),
  H2('README.md of the repository'),
  ...codeLines(rd('README.md'), 14),
  H2('GitHub steps'),
  ...codeLines(rd('GITHUB_STEPS.md'), 14),

  H1('Appendix B – Test Harness, Shims and Benchmarks'),
  P('These files are **not part of the applications**. They exist so that the real GUI classes could be executed on a server without a display. The application sources use only the standard Java API and were also compiled with the plain JDK (javac) without the shims.'),
  H2('B.1 Headless shims and how the harness runs'),
  ...codeFile('tools/run_headless.sh'), ...codeFile('tools/headless-shims/java/awt/Frame.java', 'test-only stand-in for the native Frame'),
  ...codeFile('tools/headless-shims/java/awt/Label.java'), ...codeFile('tools/headless-shims/javax/swing/JOptionPane.java', 'records dialogs instead of showing them'),
  ...codeFile('tools/harness/HeadlessRender.java'), ...codeFile('tools/harness/FrameRender.java'),
  H2('B.2 Task harnesses (drive the real classes with real events)'),
  ...codeFile('Task01/FeeHarness.java'), ...codeFile('Task02/AppointmentHarness.java'), ...codeFile('Task03/ResizeHarness.java'),
  ...codeFile('Task04/DrawPadHarness.java'), ...codeFile('Task05/ConverterHarness.java'), ...codeFile('Task06/test/RegistrationHarness.java'),
  H2('B.3 Benchmarks used in the alternative-concept analyses'),
  ...codeFile('Task01/DispatchBenchmark.java'), ...codeFile('Task02/CacheBenchmark.java'), ...codeFile('Task03/LayoutBenchmark.java'),
  ...codeFile('Task04/RepaintBenchmark.java'), ...codeFile('Task06/test/PaginationBenchmark.java'), ...codeFile('Task06/sql/sql_logic_check.py'),

  H1('Appendix C – Traceability and Viva Preparation'),
  H2('C.1 Requirement-to-evidence table'),
  table(['Task', 'Requirement / defect', 'Evidence in the report'], [
    ['1', 'D1–D10 diagnosed; repaired design', 'Table 1.1, Figure 1.1, test output, Figures 1.3–1.7'],
    ['1', 'Window stays responsive (D5)', 'Output [4] of FeeHarness: 32 pings, slowest 1.28 ms; Figure 1.6'],
    ['1', 'Alternative: listener per button vs chain', 'Table 1.2, Figure 1.8'],
    ['2', 'Event model, hierarchy, pseudocode, trace', 'Tables 2.1–2.2, Figures 2.1–2.2, pseudocode, harness output'],
    ['2', 'Alternative: cache vs query', 'Table 2.3, Figure 2.10'],
    ['3', 'Results grow, search row and buttons keep size', 'resize_output.txt, Table 3.1, Figures 3.2–3.4'],
    ['3', 'Alternative: GridBag / absolute', 'Figures 3.5–3.7, Table 3.2'],
    ['4', 'Wrong method name; failing interaction', 'Output section 1, Figures 4.1–4.3'],
    ['4', 'Complete the Code (TODO 1 and 2); @Override experiment', 'Figures 4.5–4.6; compiler outputs (1) and (2)'],
    ['4', 'Alternative: BufferedImage', 'Table 4.1, Figure 4.7'],
    ['5', 'Four styles; changed requirement; comparison', 'Converter output, Table 5.1, Table 5.2'],
    ['5', 'Complete the Code; alternatives E and F', 'Figures 5.1–5.2; Table 5.3'],
    ['6', 'REQ-01 … REQ-12', 'Table 6.1 with test identifiers; Figures 6.6–6.13; ServiceDemo output'],
    ['6', 'Alternative: all rows at once', 'Table 6.3, Figure 6.14, SQL check output'],
  ], [700, 4200, 4126], { leftAll: true, size: 16 }),
  H2('C.2 Viva preparation – short answers'),
  table(['Question', 'Answer'], [
    ['Why is e.getActionCommand() == "Pay" wrong?', '== compares references; two equal strings can be different objects. Use equals() – or better, give every button its own listener or Action.'],
    ['Why must JDBC not run in the listener?', 'The listener runs on the event-dispatch thread; a slow call freezes painting and events. Use SwingWorker: doInBackground() off the EDT, done() on the EDT.'],
    ['What is the difference between ItemEvent and ActionEvent on a JComboBox?', 'ItemEvent fires twice (DESELECTED, SELECTED) and tells the state change; ActionEvent fires once per user selection.'],
    ['Why BorderLayout CENTER for the results area?', 'CENTER gets all the extra space; NORTH/SOUTH keep their preferred height. FlowLayout never stretches its components.'],
    ['Why did the dots disappear after minimising?', 'They were drawn with getGraphics() and not stored. On repaint, paint() draws only what the program remembers. Store the points and draw them in paint().'],
    ['What does @Override give?', 'The compiler checks that the method really overrides: mouseclicked (small c) gives “method does not override or implement a method from a supertype”.'],
    ['Which listener style for a large application?', 'Lambdas/method references that call controller methods, and one shared Action for commands with several triggers; not “frame implements ActionListener”.'],
    ['How is a duplicate registration prevented?', 'Service pre-check for a friendly message, and the UNIQUE (reg_no, event_id) key as the final guard (it catches two clients registering at the same time).'],
    ['Why paging with LIMIT / OFFSET?', 'Fetch time, memory and the time the EDT is blocked depend on the page size, not on the table size; deep OFFSET still costs O(offset) – keyset paging fixes that.'],
  ], [3300, 5726], { leftAll: true, size: 16 }),
  H2('C.3 Final checklist before submission'),
  B('Replace every <placeholder> (name, register number, college, GitHub link, date).'),
  B('Run every program on your own computer; retake the screenshots from the real windows; run WindowTrace and paste its output in Task 2.4.'),
  B('Create the MySQL database with campus_events.sql, run the JDBC version of Task 6 and add one screenshot of the data in MySQL.'),
  B('Create the GitHub repository CSA09-<RegisterNumber>-Assignment4 and commit task by task.'),
);

// ---------------------------------------------------------------- document
const doc = new Document({
  creator: 'Student', title: 'CSA09 Assignment 4 Report',
  styles: {
    default: { document: { run: { font: 'Calibri', size: 22 } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: 'Calibri', size: 32, bold: true, color: '1F3864' }, paragraph: { spacing: { before: 240, after: 160 }, keepNext: true, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: 'Calibri', size: 26, bold: true, color: '2F5496' }, paragraph: { spacing: { before: 220, after: 100 }, keepNext: true, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: 'Calibri', size: 23, bold: true, color: '404040' }, paragraph: { spacing: { before: 160, after: 80 }, keepNext: true, outlineLevel: 2 } },
    ],
  },
  numbering: { config: [{ reference: 'bul', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
  sections: [
    { properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 360, right: 360, bottom: 360, left: 360 } } }, children: assignmentPages },
    { properties: { type: SectionType.NEXT_PAGE, page: { size: { width: 11906, height: 16838 }, margin: { top: 1300, right: 1440, bottom: 1200, left: 1440 }, pageNumbers: { start: 1 } } },
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'CSA09 – Programming in Java – Assignment 4 (CO4)   |   Page ', size: 18 }), new TextRun({ children: [PageNumber.CURRENT], size: 18 })] })] }) },
      children: body },
  ],
});
Packer.toBuffer(doc).then(b => { fs.writeFileSync(path.join(ROOT, 'report/CSA09_Assignment4_Report.docx'), b); console.log('report written', (b.length / 1048576).toFixed(1) + ' MB'); });
