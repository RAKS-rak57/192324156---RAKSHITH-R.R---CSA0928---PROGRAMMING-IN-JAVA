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


// ---------------------------------------------------------------- SECTION 0: the assignment as issued (FIRST pages)
const assignmentPages = [];
for (let i = 1; i <= 10; i++) {
  const buf = fs.readFileSync(path.join(ROOT, `assignment/pg-${String(i).padStart(2, '0')}.png`));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  const W = 740, Hh = Math.round(W * h / w);
  assignmentPages.push(new Paragraph({ spacing: { before: 0, after: 0 }, alignment: AlignmentType.CENTER,
    children: [new ImageRun({ type: 'png', data: buf, transformation: { width: W, height: Math.min(Hh, 1066) }, altText: { title: 'Assignment page ' + i, description: 'Assignment 5 page ' + i, name: 'assignment' + i } })] }));
}
const body = [];
const add = (...x) => x.forEach(e => Array.isArray(e) ? e.forEach(y => body.push(y)) : body.push(e));
const fileKB = f => (fs.statSync(path.join(ROOT, f)).size / 1024).toFixed(0);

// ---------------------------------------------------------------- COVER + FRONT MATTER
add(
  new Paragraph({ spacing: { before: 1500 }, alignment: AlignmentType.CENTER, children: [new TextRun({ text: '<YOUR COLLEGE / DEPARTMENT NAME>', bold: true, size: 28 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 500 }, children: [new TextRun({ text: '(write your college and department here)', italics: true, size: 20 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: [new TextRun({ text: 'CSA09 – PROGRAMMING IN JAVA', bold: true, size: 40 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: [new TextRun({ text: 'ASSIGNMENT 5 – CO5 REPORT', bold: true, size: 34 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 }, children: [new TextRun({ text: 'Persistence: Files, Streams, Serialization and JDBC – Design, Implementation and Experimental Investigation', size: 24 })] }),
  table(['Item', 'Details'], [
    ['Student name', '<YOUR NAME>'], ['Register number', '<REGISTER NUMBER>'],
    ['Course', 'CSA09 – Programming in Java'], ['Assignment', 'Assignment 5 – CO5 (Part A 25 marks + Part B 25 marks = 50 marks)'],
    ['Java version used', 'OpenJDK 21 (java -version)'],
    ['GitHub repository', 'https://github.com/<your-username>/CSA09-<RegisterNumber>-Assignment5'],
    ['Submission date', '<DATE>'],
  ], [2600, 6426], { leftAll: true, size: 21 }),
  gap(),
  P('The pages before this cover page are the assignment as issued. The answers follow in the order of the assignment: **Part A** (design and pseudocode: A1–A5) and **Part B** (implementation and experiments: B1–B4). Every answer is placed under the marked sub-question it belongs to, and the **Marks Map** on the next pages shows, for all 50 marks, where each item is answered and what evidence supports it.'),

  H1('Contents'),
  table(['Part / task', 'Topic', 'Marks'], [
    ['Marks Map', 'Rubric alignment: every marked sub-question → section, evidence and figures', '–'],
    ['Part A', 'Persistence Design & Pseudocode', '25'],
    ['A1', 'Design a File Format (bus-pass records)', '7'],
    ['A2', 'Don\'t Read What You Don\'t Need (buffered I/O)', '5'],
    ['A3', 'Discover the Data-Access Design Pattern (DAO)', '5'],
    ['A4', 'What Should Be Serialized?', '4'],
    ['A5', 'Design for Correctness and Safety (transactions, injection)', '4'],
    ['Part B', 'Implementation & Experimental Investigation', '25'],
    ['B1', 'Implement and Instrument the File Store (+ Complete the Code: PassStore)', '7'],
    ['B2', 'Engineer an Adversarial Input', '5'],
    ['B3', 'Statement vs PreparedStatement vs Batch (+ Complete the Code: PassLoader)', '8'],
    ['B4', 'Final Engineering Decision (12 campuses)', '5'],
    ['Appendix A', 'Repository layout, how to run (also with MySQL), GitHub steps', '–'],
    ['Appendix B', 'The test-only JDBC driver and how the evidence was produced', '–'],
    ['Appendix C', 'Complete list of programs, viva preparation, final checklist', '–'],
  ], [1700, 6326, 1000], { leftAll: true, size: 19 }),
  gap(),
  H2('How the evidence in this report was produced (please read)'),
  P('The assignment states that **execution screenshots alone are insufficient**: design reasoning, measurements and interpretation are assessed. Every program in this report was compiled and executed (OpenJDK 21), and every number in the tables is read from a file written by a program (CSV or console output). Two limitations of the preparation machine must be stated openly:'),
  table(['Topic', 'What was done', 'Consequence for the reader'], [
    ['Database', 'The machine has no MySQL server and no Connector/J jar. All JDBC programs use only the standard java.sql interfaces and were run against the real SQLite engine through a small **test-only JDBC driver** (jdbc:minisqlite:, Appendix B, built on Java\'s foreign-function API). SQL scripts are written in the common subset that MySQL and SQLite both accept.', 'Statement / PreparedStatement / batch / commit / rollback / constraints / ResultSetMetaData / DatabaseMetaData behave as in JDBC. Not measured: network round trips, MySQL\'s rewriteBatchedStatements, LOAD DATA, MySQL-specific escaping (backslash). Each place where this matters is marked in the text. For the final submission: run the same programs with the MySQL driver (only the JDBC URL changes).'],
    ['Pseudocode standard', 'The text of “SIMATS Engineering – Standard for Algorithmic Pseudocode” was not available. Pseudocode in Part A uses numbered steps and the keywords ALGORITHM, INPUT, OUTPUT, BEGIN, END, IF/THEN/ELSE/ENDIF, FOR EACH/ENDFOR, WHILE/ENDWHILE, TRY/CATCH, CALL, SET, RETURN.', 'If the official standard prescribes other keywords, only the keywords need to change. Part A contains pseudocode only, as required; the matching Java is in Part B and the appendix.'],
    ['Timings', 'Measured on one Linux machine with a real disk (commits are made durable). Repeats and warm-up are stated for each experiment.', 'Absolute times will differ on another computer; trends and operation counts do not.'],
  ], [1700, 4300, 3026], { leftAll: true, size: 15 }),
  gap(),
  P('**For the final submission:** run every program on your own computer (the assignment requires individual execution), re-run the JDBC programs with MySQL, retake the screenshots, replace the placeholders, and commit to GitHub task by task.'),

  H1('Marks Map – Rubric Alignment (50 marks)'),
  P('The assignment awards marks per sub-question and states the **evidence expected** for each task; it does not publish band descriptors. The table below therefore uses (1) the sub-question marks, (2) the “Evidence Expected” line of the task, and (3) the “How to answer” rules (Complete the Code: execute it and compare with the expected output; Alternative Concept: describe the alternative with code or pseudocode, analyse time and space in Big-O, and support it with your own measurements; Bloom level K4 = analyse, K5 = evaluate). “Where” gives the section of this report.'),
  table(['Task', 'Sub-question', 'Marks', 'What must be shown (criterion)', 'Where in this report', 'Evidence'], [
    ['A1', 'A1.1 Represent the record', '2', 'Two layouts (text and DataOutputStream): field order, delimiter / sizes, name containing the delimiter', 'A1.1, Figures A1.1–A1.2, Table A1.1', 'Design + verified in B1 tests'],
    ['A1', 'A1.2 Design the operations', '3', 'Pseudocode for add / find / list; stream classes chosen and justified', 'A1.2, pseudocode, Table A1.2', 'Pseudocode + B1 implementation'],
    ['A1', 'A1.3 Design for damaged data', '2', 'Damaged line, missing file, incomplete write (power failure)', 'A1.3, Table A1.3', 'Design + B1 tests T0, T7–T10, B2 file demo'],
    ['A1', 'Alternative: RandomAccessFile', 'in task', 'Solution, O(n) vs O(1), padding waste', 'A1 Alternative, Figure A1.3, Table A1.4', 'Measured (B1.4 growth data)'],
    ['A2', 'A2.1 Predict the cost', '2', 'Number of read calls and why slow, with reasons', 'A2.1', 'Prediction checked in B1.3'],
    ['A2', 'A2.2 Redesign using buffering', '2', 'Buffered stream or byte[] buffer; change in operating-system calls', 'A2.2, Figure A2.1, Table A2.1', 'Measured call counts + times'],
    ['A2', 'A2.3 When buffering does not help', '1', 'One justified situation', 'A2.3', 'Measured (two situations)'],
    ['A2', 'Alternative: Files.readAllLines()', 'in task', 'O(file size) vs O(buffer) memory, when each is appropriate', 'A2 Alternative, Table A2.2', 'Measured time and memory'],
    ['A3', 'A3.1 Common pattern', '2', 'Repeated steps and the parts that differ', 'A3.1, Table A3.1', 'Analysis'],
    ['A3', 'A3.2 Generic Dao<T, K>', '2', 'insert, findById, findAll, update, delete; StudentDao', 'A3.2, Figure A3.1, pseudocode', 'Design + runnable Java in Part B/appendix'],
    ['A3', 'A3.3 Object–table mapping', '1', 'Student fields → student columns, primary key', 'A3.3, Table A3.2, ER diagram', 'Design + ER'],
    ['A3', 'Alternative: SQL in services', 'in task', 'Places to change on a column rename; time and space per operation', 'A3 Alternative, Tables A3.3–A3.4', 'Counted from source + measured'],
    ['A4', 'A4.1 Decide what to persist', '2', 'transient fields and why; object-graph behaviour', 'A4.1, Figure A4.1, Table A4.1', 'Reasoned + executed demo'],
    ['A4', 'A4.2 Plan for change', '2', 'Old file read by new class; role of serialVersionUID', 'A4.2, Table A4.2', 'Executed experiment (two class versions)'],
    ['A4', 'Alternative: CSV / JSON', 'in task', 'File size, save/load time, new-field behaviour', 'A4 Alternative, Table A4.3, Figure A4.2', 'Measured'],
    ['A5', 'A5.1 Transaction design', '2', 'JDBC commit / rollback pseudocode; no money lost or created', 'A5.1, pseudocode, Figure A5.1', 'Pseudocode + executed in A5.3'],
    ['A5', 'A5.2 Injection analysis', '2', 'Crafted input; PreparedStatement redesign', 'A5.2, Figure A5.2, pseudocode', 'Reasoned + executed in B2'],
    ['A5', 'Alternative: two auto-commit updates', 'in task', 'Outcome on failure; time cost; data risk', 'A5 Alternative, Table A5.1', 'Executed failure + timing'],
    ['B1', 'B1.1 Validate the implementation', '2', 'Tests: normal, id not found, damaged line; what each verifies', 'B1.1, Table B1.1', 'Executed test table (11 tests)'],
    ['B1', 'B1.2 Compare with the design', '1', 'Changes made to the Part A pseudocode and why', 'B1.2, Table B1.2', 'Analysis'],
    ['B1', 'B1.3 Measure buffering', '2', '1, 10, 50 MB, unbuffered vs buffered; compare with A2.1', 'B1.3, Table B1.3, Figure B1.1', 'Measured'],
    ['B1', 'B1.4 Measure growth', '2', '1,000 → 100,000 records; explain trend', 'B1.4, Table B1.4, Figure B1.2', 'Measured'],
    ['B1', 'Complete the Code: PassStore', 'in task', 'TODO 1; output; why damaged P1002 does not stop the search', 'B1 Complete the Code', 'Executed, output equals expected'],
    ['B1', 'Alternative: HashMap', 'in task', 'O(q·n) vs O(n+q); memory of both', 'B1 Alternative, Table B1.5, Figure B1.3', 'Measured'],
    ['B2', 'Adversarial login input', '5 (total for B2)', 'Input that fools the unsafe check; PreparedStatement rejects it', 'B2.1, Tables B2.1–B2.2', 'Executed against a real SQL engine'],
    ['B2', 'Damaged bus-pass file', '(B2)', 'Wrong field count, non-numeric amount, empty line: store response', 'B2.2, Table B2.3', 'Executed'],
    ['B2', 'What each input reveals', '(B2)', 'Explanation per input', 'B2.3, Table B2.4', 'Reasoned'],
    ['B2', 'Alternative: escaping / regex', 'in task', 'Why weaker; validation cost vs PreparedStatement', 'B2 Alternative, Tables B2.5–B2.6, Figure B2.1', 'Executed bypasses + measured'],
    ['B3', 'B3.1 Correctness on small data', '2', '10 records per approach; SELECT and ResultSetMetaData', 'B3.1', 'Executed, 3/3 pass'],
    ['B3', 'B3.2 Increase the data size', '2', '1,000 / 10,000 / 100,000 rows × Statement, PreparedStatement, batch', 'B3.2, Table B3.1, Figure B3.1', 'Measured'],
    ['B3', 'B3.3 Auto-commit vs transaction', '2', 'Batch with both modes; failure half-way; table contents', 'B3.3, Tables B3.2–B3.3', 'Measured + executed failures'],
    ['B3', 'B3.4 Engineering trade-off', '2', 'Choice for (a) nightly import of 100,000, (b) one pass at the counter', 'B3.4', 'Reasoned from the measurements'],
    ['B3', 'Complete the Code: PassLoader', 'in task', 'TODO 1–3, loadAll with 1,000 / 10,000 / 100,000, invalid amount half-way → no rows', 'B3 Complete the Code', 'Executed'],
    ['B3', 'Alternative: multi-row INSERT / LOAD DATA', 'in task', 'Time for n rows, memory for a batch of b rows', 'B3 Alternative, Table B3.4', 'Measured (LOAD DATA: design only)'],
    ['B4', 'B4.1 Practical strategy', '2', 'Migration with batches + transaction; failure report', 'B4.1, migration output', 'Executed'],
    ['B4', 'B4.2 Inspect the database', '1', 'DatabaseMetaData: product, driver version, tables; why useful', 'B4.2', 'Executed'],
    ['B4', 'B4.3 Production decision', '2', 'Text vs serialization vs database, justified by measurements and requirements', 'B4.3, Tables B4.1–B4.2, Figure B4.1', 'Measured + executed concurrency'],
    ['B4', 'Alternative: one serialized file per campus', 'in task', 'Search time, space, concurrent counters in each design', 'B4 Alternative', 'Measured + executed concurrency'],
  ], [600, 2200, 700, 2650, 1700, 1176], { leftAll: true, size: 14 }),
  caption('Table M.1 – Marks Map. Part A: 7 + 5 + 5 + 4 + 4 = 25 marks. Part B: 7 + 5 + 8 + 5 = 25 marks. Total 50.'),
  gap(),
  P('**Reading guide for an evaluator.** In each task, the sub-questions appear in the assignment order with their marks in the heading. Part A contains the design reasoning and pseudocode (no Java in place of pseudocode, as the assignment requires); Part B contains the implementations (full source listings), the measurements (tables and figures generated from CSV files written by the programs), and the interpretation of each result.'),
);

// =====================================================================================================
// data used in several places
// =====================================================================================================
const buf = csv('TaskB1/buffering_results.csv');
const B50 = m => buf.find(r => r.size_MB === '50' && r.method === m);
const grow = csv('TaskB1/growth_results.csv');
const many = csv('TaskB1/search_many_results.csv');
const fmts = csv('TaskA4/format_results.csv');
const grab = (file, re) => { const m = rd(file).match(re); return m ? m : null; };
const nb = grab('TaskB1/buffering_output.txt', /\(a\) reads of 64 KB blocks: FileInputStream ([0-9.]+) ms \(([0-9,]+) calls\)\s+\|\s+wrapped in BufferedInputStream ([0-9.]+) ms \(([0-9,]+) calls\)/);
const nb2 = grab('TaskB1/buffering_output.txt', /plain ([0-9.]+) ms\s+\|\s+buffered ([0-9.]+) ms/);
const ral = grab('TaskB1/readalllines_memory_output.txt', /readAllLines : ([0-9]+) ms.*?retained by the list = ([0-9]+) MB, allocated while reading = ([0-9]+) MB/);
const g100 = grow[grow.length - 1];
// average number of bytes of real content in a record of the benchmark data (to compute the padding of fixed-length records)
let contentSum = 0; const NG = 100000;
for (let i = 0; i < NG; i++) contentSum += String(1001 + i).length + 1 + 9 + ('Student Name '.length + String(i % 1000).length) + 4 + ('Stop '.length + String(i % 25).length) + 10 + 8;
const contentAvg = contentSum / NG;
const textPerRec = g100.text_bytes / g100.n;

// =====================================================================================================
// PART A
// =====================================================================================================
add(
  H1('PART A – Persistence Design & Pseudocode (25 marks)'),
  P('All pseudocode follows the numbered-step style described in the front matter. Part A contains design reasoning and pseudocode only; the corresponding Java programs and all measurements are in Part B. Where a Part A claim is verified by an experiment, the text says which one.'),

  // ------------------------------------------------------------------ A1
  H1('Task A1 – Design a File Format (7 marks, K4)'),
  P('**Context:** the transport office issues about 8,000 bus passes a year. A pass has: pass ID, student register number, name, route number, boarding stop, validity date, amount paid. The office must (i) add a pass, (ii) find a pass by pass ID, (iii) list all passes on a route.'),
  H2('A1.1 – Represent the record (2 marks)'),
  H3('(a) Delimited text file'),
  ...fig('diagrams/a1_text_layout.png', 540, 280, 'Figure A1.1 – Text layout: one pass = one line, eight comma-separated fields'),
  table(['#', 'Field', 'Type / format', 'Example', 'Check on reading'], [
    ['1', 'passId', 'text, e.g. P1001', 'P1001', 'not empty'],
    ['2', 'regNo', 'text (9 digits)', '192311001', '–'],
    ['3', 'name', 'text, may contain a comma', 'Kumar\\, Meena', 'escapes removed after splitting'],
    ['4', 'route', 'integer', '12', 'must be an integer'],
    ['5', 'stop', 'text', 'Main Gate', '–'],
    ['6', 'validity', 'date yyyy-mm-dd', '2027-06-30', 'must be a real date'],
    ['7', 'amount', 'decimal with 2 places', '4500.00', 'must be a positive number'],
    ['8', 'crc32', 'checksum of fields 1–7 (hex)', '84b0f00e', 'must match (detects damage)'],
  ], [500, 1500, 2700, 2100, 2226], { leftAll: true, size: 16 }),
  caption('Table A1.1 – Field order of the text record'),
  P('**Delimiter and names that contain the delimiter.** The delimiter is the comma and the record ends with a line break. Inside a field, a comma is written as \\, a backslash as \\\\ and a line break as \\n (backslash escaping). The reader splits only at commas that are not preceded by an escaping backslash and then removes the escapes. The name “Kumar, Meena” is therefore stored as Kumar\\, Meena and is read back unchanged (verified in test T2 of Task B1). Without escaping, the line would split into nine fields and the record would be lost or mis-read.'),
  H3('(b) Binary file written with DataOutputStream'),
  ...fig('diagrams/a1_binary_layout.png', 560, 300, 'Figure A1.2 – Binary layout (variable-length records)'),
  P('Fields are written in this order: writeUTF(passId), writeUTF(regNo), writeUTF(name), writeInt(route), writeUTF(stop), writeLong(validity as epoch day), writeDouble(amount). writeUTF writes a 2-byte length followed by the text, so **no delimiter is needed and a name containing a comma needs no special handling**; the price is that the file cannot be read with a text editor and the reader must read the fields in exactly the same order. A typical record is about 65 bytes (against about 72 bytes for the text line measured in Task B1).'),

  H2('A1.2 – Design the operations (3 marks)'),
  ...code(`
ALGORITHM AddPass
INPUT   : pass (passId, regNo, name, route, stop, validity, amount), the pass file
OUTPUT  : the pass is appended as the last line, or an error is reported
BEGIN
  1.  IF any field is invalid (empty passId, route not an integer, impossible date, amount <= 0) THEN RETURN error ENDIF
  2.  IF FindPass(passId) is not "not found" THEN RETURN error "duplicate pass id" ENDIF       // O(n) scan
  3.  SET line <- join with commas: escape(passId), escape(regNo), escape(name), route, escape(stop), validity, amount with 2 decimals
  4.  SET line <- line + "," + crc32(line)
  5.  IF the file exists AND its last byte is not a line break THEN append a line break ENDIF   // repair after a half-written line
  6.  OPEN a BufferedWriter on the file in APPEND mode, creating the file if it is missing      // try-with-resources
  7.  WRITE line followed by a line break in ONE write call
  8.  CLOSE the writer                                                                           // flushes the buffer
END

ALGORITHM FindPass
INPUT   : passId, the pass file
OUTPUT  : the pass, or "not found"
BEGIN
  1.  IF the file does not exist THEN RETURN "not found" ENDIF
  2.  OPEN a BufferedReader on the file                                                          // try-with-resources
  3.  SET lineNo <- 0
  4.  WHILE readLine() returns a line DO
  5.      SET lineNo <- lineNo + 1
  6.      TRY    SET pass <- Parse(line)          // split at unescaped commas, expect 8 fields, check crc32, convert route / date / amount
  7.      CATCH DamagedRecord(reason)    CALL report("line " + lineNo + ": " + reason);  CONTINUE with the next line    ENDTRY
  8.      IF pass.passId = passId THEN CLOSE the reader; RETURN pass ENDIF
  9.  ENDWHILE
  10. CLOSE the reader;  RETURN "not found"
END

ALGORITHM ListByRoute
INPUT   : route number, the pass file
OUTPUT  : list of all passes whose route equals the given route
BEGIN
  1.  SET result <- empty list
  2.  OPEN a BufferedReader on the file (if the file is missing, RETURN result)
  3.  WHILE readLine() returns a line DO
  4.      TRY SET pass <- Parse(line)   CATCH DamagedRecord  report and CONTINUE   ENDTRY
  5.      IF pass.route = route THEN add pass to result ENDIF                       // no early exit: every line must be examined
  6.  ENDWHILE
  7.  CLOSE the reader;  RETURN result
END`),
  gap(),
  table(['Operation', 'Stream classes chosen', 'Why this choice (and what was rejected)'], [
    ['Add', 'Files.newBufferedWriter(path, UTF-8, CREATE, APPEND) = BufferedWriter over OutputStreamWriter over FileOutputStream', 'The record is text, so a character writer is natural; the charset is stated explicitly (FileWriter used the platform default charset in older Java versions, so a file could be unreadable on another computer). APPEND means existing data is never overwritten. BufferedWriter collects the characters so the record reaches the operating system in one write. FileOutputStream alone writes bytes and would need manual encoding.'],
    ['Find / List', 'Files.newBufferedReader(path, UTF-8) = BufferedReader over InputStreamReader over FileInputStream', 'readLine() returns one record at a time, which is exactly the unit of this format. The buffer reduces operating-system reads from one per byte to one per 8 KB (measured in Task A2/B1.3). A plain FileInputStream.read() or FileReader.read() per character would be thousands of times slower for no gain.'],
    ['Repair check before add', 'RandomAccessFile (read the last byte)', 'Reading the last byte needs a seek, which stream classes do not offer; it costs O(1).'],
  ], [1500, 3000, 4526], { leftAll: true, size: 16 }),
  caption('Table A1.2 – Stream choices and justification'),

  H2('A1.3 – Design for damaged data (2 marks)'),
  table(['Problem', 'How it is detected', 'How it is handled'], [
    ['A damaged line (edited by hand, cut, wrong number of fields, bad number or date)', 'Parse() checks four things in order: the number of fields (8), the CRC-32 of the first seven fields, the integer route, the date and the positive amount. Any failure throws DamagedRecord(reason).', 'The line is skipped and reported with its line number and reason; the scan **continues** with the next line, so one bad line never hides the good records. The report is available to the caller (verified: Task B1 tests T7–T8 and the nine damaged lines of Task B2).'],
    ['A missing file', 'Files.exists(path) is false (or NoSuchFileException).', 'find() returns “not found” and list() returns an empty list: a missing file means “no passes yet”, not a crash. add() creates the file (CREATE option). Verified: test T0.'],
    ['An incomplete write caused by a power failure', 'The last line is cut: it fails the field count or the CRC check, and the file does not end with a line break.', 'Each record is written with one write call, so a power cut can damage only the LAST line, never an earlier one. The damaged last line is skipped on reading. Before the next add, the program checks the last byte of the file and, if it is not a line break, writes one, so the new record does not get glued to the broken line (verified: test T10). Operations that rewrite the whole file (delete / update) write a temporary file and replace the old one with an atomic move, so there is always one complete file.'],
  ], [2300, 3200, 3526], { leftAll: true, size: 16 }),
  caption('Table A1.3 – Design for damaged data'),
  P('**Limit of the design.** Closing the writer hands the data to the operating system, not necessarily to the disk. A stronger guarantee needs FileChannel.force(true) after every record; Task B3 shows what a durable commit costs on this disk (about 0.8 ms per commit), which is why the design does not force every record.'),

  H2('Alternative Concept and Complexity Analysis – RandomAccessFile with fixed-length records'),
  ...fig('diagrams/a1_fixed_layout.png', 560, 280, 'Figure A1.3 – Fixed-length record layout (92 bytes)'),
  ...code(`
ALGORITHM FindPassFixed
INPUT   : passId (for example "P1003"), the record file, RECORD_SIZE = 92
OUTPUT  : the pass, or "not found"
BEGIN
  1.  SET i <- (number in passId) - 1001                      // pass ids are dense: P1001 is record 0
  2.  IF i < 0 OR (i + 1) * RECORD_SIZE > file length THEN RETURN "not found" ENDIF
  3.  CALL seek(i * RECORD_SIZE)
  4.  CALL readFully(record of RECORD_SIZE bytes)
  5.  IF the passId field of the record is not passId THEN RETURN "not found" ENDIF   // empty slot
  6.  RETURN decode(record)
END
ALGORITHM AddPassFixed:  SET i from passId; seek(i * RECORD_SIZE); write the 92 bytes padded with spaces.`),
  gap(),
  P(`**Time.** The delimited file must be scanned: O(n) lines. The fixed-length file needs one seek and one read of 92 bytes: **O(1)**. Measured in Task B1.4 (n = 1,000 → 100,000): the scan grew from ${f2(grow[0].scan_last_ms)} ms to ${f1(g100.scan_last_ms)} ms while the seek stayed at ${f1(grow[0].fixed_seek_us)}–${f1(Math.max(...grow.map(r => Number(r.fixed_seek_us))))} µs for every n (Table B1.4).`),
  P(`**Space wasted by padding.** Every record occupies 92 bytes even if the fields are short. In the benchmark data the real content of a record averages ${contentAvg.toFixed(0)} bytes, so **${(92 - contentAvg).toFixed(0)} bytes (${((92 - contentAvg) / 92 * 100).toFixed(0)}%) of every record is padding**. The text line needs only ${textPerRec.toFixed(0)} bytes on average (measured: ${fmt(g100.text_bytes)} bytes for 100,000 passes against ${fmt(g100.fixed_bytes)} bytes for the fixed file, i.e. the fixed file is ${((g100.fixed_bytes / g100.text_bytes - 1) * 100).toFixed(0)}% larger). For 8,000 passes the difference is only about ${((8000 * (g100.fixed_bytes / g100.n - g100.text_bytes / g100.n)) / 1024).toFixed(0)} KB, so the space cost is small in absolute terms.`),
  table(['', 'Delimited text file', 'Fixed-length RandomAccessFile'], [
    ['Find by pass ID', 'O(n) scan', 'O(1) seek (needs dense, numbered ids)'],
    ['List by route', 'O(n) scan', 'O(n) scan (every record must still be read)'],
    ['Space', 'only the real content (about 72 bytes per record)', '92 bytes per record, padding about 36%; fixed field widths (a longer name is cut)'],
    ['Delimiters inside data', 'need escaping', 'not an issue'],
    ['Deleting a record', 'rewrite the file', 'leaves a hole (needs a free-slot marker)'],
    ['Human readable', 'yes', 'no'],
  ], [2000, 3500, 3526], { leftAll: true, size: 16 }),
  caption('Table A1.4 – Comparison. Conclusion: the fixed-length file is better only when pass ids are dense and lookup by id dominates; for the requirement “list by route” both designs scan.'),

  // ------------------------------------------------------------------ A2
  H1('Task A2 – Don\'t Read What You Don\'t Need (5 marks, K4)'),
  P('A program reads a 50 MB attendance log one byte at a time with FileInputStream.read() to count the lines.'),
  H2('A2.1 – Predict the cost (2 marks)'),
  P(`**Prediction.** A 50 MB file has 52,428,800 bytes. FileInputStream.read() returns one byte per call and the last call returns −1, so the program makes **about 52.4 million read calls (file size + 1)**. Each call crosses from Java into native code and into the operating system (a system call), which costs a few hundred nanoseconds even though only one byte is delivered; 52 million calls therefore cost several seconds to tens of seconds, and almost all of the time is call overhead, not disk transfer. I predicted a time of roughly 10 s (about 200 ns per call). **Checked in Task B1.3:** the real log file of ${fmt(B50('unbuffered read()').file_bytes)} bytes caused **${fmt(B50('unbuffered read()').read_calls)} read calls** (exactly file size + 1) and took ${f1(B50('unbuffered read()').time_ms / 1000)} s, i.e. ${(B50('unbuffered read()').time_ms * 1e6 / B50('unbuffered read()').read_calls).toFixed(0)} ns per call. The number of calls was predicted exactly and the time within the predicted order of magnitude.`),
  H2('A2.2 – Redesign using buffering (2 marks)'),
  ...code(`
ALGORITHM CountLinesBuffered
INPUT   : file name
OUTPUT  : number of line breaks
BEGIN
  1.  SET buffer <- array of 65,536 bytes;  SET lines <- 0
  2.  OPEN a FileInputStream on the file                                       // try-with-resources
  3.  REPEAT
  4.      SET n <- read(buffer)                                                // ONE system call fills up to 65,536 bytes
  5.      FOR i FROM 0 TO n - 1 DO  IF buffer[i] is a line break THEN lines <- lines + 1 ENDIF  ENDFOR
  6.  UNTIL n = -1
  7.  CLOSE the stream;  RETURN lines
END
// Variant: wrap the FileInputStream in a BufferedInputStream and keep the one-byte read(): the program still calls read()
// 52 million times, but each call is served from a memory buffer; the operating system is asked only once per 8,192 bytes.`),
  gap(),
  ...fig('diagrams/a2_streams.png', 570, 340, 'Figure A2.1 – How buffering changes the number of operating-system reads'),
  table(['Version (50 MB)', 'Calls to the file stream (= operating-system reads)', 'Time', 'Calls compared with the original'], [
    ['Original: FileInputStream.read()', fmt(B50('unbuffered read()').read_calls), f1(B50('unbuffered read()').time_ms) + ' ms', '1'],
    ['BufferedInputStream (8,192-byte buffer), still read() per byte', fmt(B50('BufferedInputStream').read_calls), f1(B50('BufferedInputStream').time_ms) + ' ms', '1 / ' + fmt(Math.round(B50('unbuffered read()').read_calls / B50('BufferedInputStream').read_calls))],
    ['Own byte[65,536] buffer', fmt(B50('byte[64 KB] buffer').read_calls), f1(B50('byte[64 KB] buffer').time_ms) + ' ms', '1 / ' + fmt(Math.round(B50('unbuffered read()').read_calls / B50('byte[64 KB] buffer').read_calls))],
  ], [3500, 2500, 1400, 1626], { leftAll: true, size: 16 }),
  caption('Table A2.1 – Operating-system reads and time (measured, Task B1.3). Calls = ceil(file size / buffer size) + 1.'),
  P('**Explanation.** Buffering changes how many times the operating system is asked: instead of one request per byte, one request per buffer. The work of examining each byte remains (O(file size)), but the expensive part, the system call, is divided by the buffer size (8,192 or 65,536). The own byte[] buffer is faster still because the program also avoids 52 million method calls into the BufferedInputStream, each of which goes through the stream\'s locking and bounds checks.'),
  H2('A2.3 – When buffering does not help (1 mark)'),
  P(`**Situation: reading in large blocks.** If the program already reads in blocks at least as large as the buffer, a BufferedInputStream passes the request straight through and adds nothing. Measured on a 10 MB file with 64 KB blocks: FileInputStream ${f1(nb[1])} ms (${nb[2]} calls) and the same stream wrapped in a BufferedInputStream ${f1(nb[3])} ms (${nb[4]} calls) – the same number of calls and no gain. **A second situation: data already in memory.** Reading a ByteArrayInputStream byte by byte took ${f1(nb2[1])} ms and wrapped in a buffer ${f1(nb2[2])} ms: there is no operating-system call to save, so the benefit is small (compare with the ${fmt(Math.round(B50('unbuffered read()').time_ms))} ms of the unbuffered 50 MB file). Buffering also does not help random access, where the next byte needed is not the next byte of the file.`),
  H2('Alternative Concept and Complexity Analysis – Files.readAllLines()'),
  ...code(`
ALGORITHM CountLinesReadAll
BEGIN
  1.  SET lines <- Files.readAllLines(path)           // the whole file becomes a List<String> in memory
  2.  RETURN size of lines
END`),
  gap(),
  table(['', 'Buffered stream / byte[] buffer', 'Files.readAllLines()'], [
    ['Time', 'O(file size): every byte is examined once', 'O(file size): every byte is decoded and copied into Strings'],
    ['Memory', 'O(buffer size): 8,192 or 65,536 bytes, independent of the file', 'O(file size): all lines are held at once'],
    ['Measured, 50 MB file (' + fmt(B50('unbuffered read()').file_bytes) + ' bytes, 1,588,751 lines)', `byte[64 KB]: ${f1(B50('byte[64 KB] buffer').time_ms)} ms, 0.009 MB allocated for the 8 KB buffer case`, `${ral[1]} ms, **${ral[2]} MB retained** (${(ral[2] / 50).toFixed(1)}× the file size), ${ral[3]} MB allocated while reading`],
    ['Appropriate when', 'big or unknown file size, one pass, limited heap (logs, imports)', 'small files, or when the program needs all lines at once (sort, random access to lines, several passes)'],
  ], [2200, 3200, 3626], { leftAll: true, size: 16 }),
  caption('Table A2.2 – Buffered streaming against reading everything (measured; programs: BufferingBenchmark, ReadAllLinesMemory)'),
  P(`The memory result is the important one: the 50 MB file needs **${ral[2]} MB** of heap as a List<String> (each of the 1.59 million lines is a separate String object with its own header and array), so a program with a 100 MB heap would fail with OutOfMemoryError on a 50 MB log, while the buffered version would not even notice the file size. readAllLines was quick here (${ral[1]} ms) because it reads the file in large blocks internally; its cost is memory, not time.`),

  // ------------------------------------------------------------------ A3
  H1('Task A3 – Discover the Data-Access Design Pattern (5 marks, K4)'),
  P('A student project has three classes (StudentDB, CourseDB, MarksDB). Each opens a connection, builds an SQL string, executes it, reads the ResultSet into objects and closes the connection; large parts are almost identical.'),
  H2('A3.1 – Identify the common pattern (2 marks)'),
  table(['Step (in every method of all three classes)', 'Repeated in all three?', 'Differs between the classes'], [
    ['1. Obtain a Connection (and close it at the end)', 'identical', '–'],
    ['2. Write the SQL text', 'same structure', 'the SQL text: table name, column names'],
    ['3. Create a Statement / PreparedStatement', 'identical', '–'],
    ['4. Bind the parameters', 'same idea', 'number, types and order of the parameters'],
    ['5. Execute (executeQuery / executeUpdate)', 'identical', '–'],
    ['6. Read the ResultSet and copy the columns into an object', 'same loop', 'the target class and the column → field mapping'],
    ['7. Handle SQLException, close ResultSet, Statement and Connection', 'identical', '–'],
  ], [4200, 1800, 3026], { leftAll: true, size: 16 }),
  caption('Table A3.1 – Common steps and differing parts. Only steps 2, 4 and 6 differ: the SQL text, the parameters and the row-to-object mapping; steps 1, 3, 5 and 7 (the “plumbing”) are identical.'),
  H2('A3.2 – A generic data-access interface (2 marks)'),
  ...fig('diagrams/a3_dao_class.png', 520, 420, 'Figure A3.1 – Dao<T, K>, the template class and three DAOs'),
  ...code(`
INTERFACE Dao<T, K>                     // T = entity class, K = type of the primary key
  insert(entity)                        // store a new row
  findById(key)  -> T or "none"
  findAll()      -> list of T
  update(entity) -> number of rows changed
  delete(key)    -> number of rows deleted
END INTERFACE

ALGORITHM Execute(sql, bindParameters, mapRow)         // written ONCE in the shared base class
BEGIN
  1.  OPEN connection;  PREPARE statement from sql                         // try-with-resources: closed automatically
  2.  CALL bindParameters(statement)
  3.  IF the statement is a query THEN
  4.      FOR EACH row IN the result DO  ADD mapRow(row) to the result list  ENDFOR
  5.  ELSE  SET rows <- statement.executeUpdate()  ENDIF
  6.  CLOSE everything;  RETURN the result list or rows
END

CLASS StudentDao IMPLEMENTS Dao<Student, String>        // supplies only what differs
  insert(s)    : CALL Execute("INSERT INTO student(reg_no, name, email, year_no) VALUES (?, ?, ?, ?)",
                              bind s.regNo, s.name, s.email, s.year)
  findById(k)  : RETURN first of Execute("SELECT reg_no, name, email, year_no FROM student WHERE reg_no = ?", bind k, mapStudent)
  findAll()    : RETURN Execute("SELECT reg_no, name, email, year_no FROM student ORDER BY reg_no", no parameters, mapStudent)
  update(s)    : RETURN Execute("UPDATE student SET name = ?, email = ?, year_no = ? WHERE reg_no = ?", bind ...)
  delete(k)    : RETURN Execute("DELETE FROM student WHERE reg_no = ?", bind k)
  mapStudent(row) : RETURN new Student(row.reg_no, row.name, row.email, row.year_no)
END CLASS                                              // CourseDao and MarksDao: only the SQL text and the mapping change`),
  gap(),
  P('The Java implementation (Dao, AbstractJdbcDao, StudentDao, CourseDao, MarksDao) is in Part B / Appendix and was executed: see the output of A3Demo below.'),
  H2('A3.3 – Map objects to tables (1 mark)'),
  table(['Student field (Java)', 'Column of table student', 'SQL type', 'Constraint'], [
    ['String regNo', 'reg_no', 'VARCHAR(12)', 'PRIMARY KEY (identifies the row; K = String in Dao<Student, String>)'],
    ['String name', 'name', 'VARCHAR(60)', 'NOT NULL'],
    ['String email', 'email', 'VARCHAR(80)', 'NOT NULL'],
    ['int year', 'year_no', 'INT', 'NOT NULL (the name “year” is avoided because it is a SQL keyword)'],
  ], [2200, 2200, 1700, 2926], { leftAll: true, size: 16 }),
  caption('Table A3.2 – Object–table mapping: one class = one table, one object = one row, one field = one column'),
  ...fig('diagrams/er_a3.png', 560, 330, 'Figure A3.2 – ER diagram of student, course and marks'),
  H2('Alternative Concept and Complexity Analysis – SQL written inside the service / GUI classes'),
  P('**The alternative solution.** The service and GUI classes contain their own SQL, each repeating the plumbing (steps 1, 3, 5, 7). The classes ScatteredStudentService, ScatteredReportScreen, ScatteredRegistrationScreen and ScatteredMarksService in Task A3 implement this alternative.'),
  ...code(`
// generic DAO: ONE place knows the table                     // scattered: every class that needs a student writes its own SQL
students.findById("192311001");                               ps = con.prepareStatement("SELECT reg_no, name, email, year_no FROM student WHERE reg_no = ?");
                                                              ps.setString(1, regNo); rs = ps.executeQuery(); ... build Student ... close all ...`),
  gap(),
  ...outFile('TaskA3/a3_output.txt', 'Output of java A3Demo (the DAO design in use, the rename count, and the per-operation cost)'),
);
{
  const a3 = rd('TaskA3/a3_output.txt');
  const gen = a3.match(/generic Dao<Student,String>.findById : ([0-9.]+) microseconds per call, ([0-9,]+) bytes/), sc = a3.match(/SQL inside the service class\s+: ([0-9.]+) microseconds per call, ([0-9,]+) bytes/);
  const gs = a3.match(/generic DAO : ([0-9]+) statements, all in ([0-9]+) class/), ss = a3.match(/scattered\s+: ([0-9]+) statements spread over ([0-9]+) classes/);
  add(
    table(['', 'Generic Dao<T, K>', 'SQL in service / GUI classes'], [
      ['Places to change when a column of the student table is renamed', `${gs[1]} SQL statements, all in **${gs[2]} class** (StudentDao)`, `${ss[1]} SQL statements spread over **${ss[2]} classes**: every class that touches the table must be found, edited and re-tested`],
      ['Time per operation (new connection each call)', `${gen[1]} µs`, `${sc[1]} µs`],
      ['Memory allocated per operation', `${gen[2]} bytes`, `${sc[2]} bytes`],
      ['Asymptotic cost', 'O(1) per statement in both designs (the same JDBC calls)', 'O(1) (the same JDBC calls)'],
    ], [3000, 3000, 3026], { leftAll: true, size: 16 }),
    caption('Table A3.3 – Comparison (counted from the source files and measured by A3Demo; the time varies between runs: 60–81 µs in the two runs made)'),
    P('**Analysis.** The number of SQL statements is almost the same in both designs (5 and 6); the difference is **where they are**: one class against four. A column rename in the DAO design means editing and re-testing StudentDao only; in the scattered design the programmer has to search every service and screen (and a missed one fails only at run time). The cost per operation is the same within the noise of the measurement because both designs perform the same JDBC calls and the time is dominated by opening the connection (about 60–80 µs); the generic design allocates about 1 KB more per call (the lambda binders, a list and the mapper), which is irrelevant next to the connection. **Conclusion:** the generic DAO costs nothing measurable and removes duplicated code and the risk of inconsistent changes.'),
  );
}

// ------------------------------------------------------------------ A4
const fm = (n, f) => fmts.find(r => Number(r.n) === n && r.format === f);
add(
  H1('Task A4 – What Should Be Serialized? (4 marks, K4)'),
  P('A hostel program saves its state by serializing a Hostel object that holds a list of Room objects, each holding a list of Resident objects. A Resident has a name, a register number, a password and a reference to a live database Connection.'),
  H2('A4.1 – Decide what to persist (2 marks)'),
  table(['Field of Resident', 'transient?', 'Reason'], [
    ['name', 'no', 'real data of the object; must survive'],
    ['regNo', 'no', 'real data of the object; must survive'],
    ['password', '**yes**', 'a secret: writing it to a file stores it in clear text where anyone who can read the file can see it (measured: the password text was found in the bytes of a non-transient version). If a password must be kept it should be a salted hash, handled separately.'],
    ['Connection conn', '**yes**', 'a live resource that is meaningless after the program ends (the socket and session no longer exist), and java.sql.Connection is **not Serializable**: with a non-transient connection the whole writeObject call fails with NotSerializableException. After reading, the field is null and the program opens a new connection.'],
  ], [1900, 1300, 5826], { leftAll: true, size: 16 }),
  caption('Table A4.1 – transient design'),
  ...fig('diagrams/a4_object_graph.png', 540, 380, 'Figure A4.1 – What writeObject(hostel) writes'),
  P('**What happens to the object graph.** Serialization follows references: writeObject(hostel) writes the Hostel, then every Room in its list, then every Resident in each room, i.e. **everything reachable from the object**, in one call. A Resident that is referenced from two rooms is written **once** and both rooms refer to the same object again after reading (a handle table records objects already written; cycles are handled in the same way). If any reachable object is not Serializable and is not reached through a transient field, the whole write fails. The executed demonstration below confirms each of these statements.'),
  ...outFile('TaskA4/serialization_output.txt', 'Output of java SerializationDemo'),
  H2('A4.2 – Plan for change (2 marks)'),
  P('A phone-number field is added to Resident after files were saved. Two versions of the class were written, compiled separately, and the old files were read with the new class (programs in Task A4/v1 and v2, run by versioning.sh):'),
  ...outFile('TaskA4/versioning_output.txt', 'Output of ./versioning.sh'),
  table(['Class declaration', 'Old file read by the new class', 'Why'], [
    ['private static final long serialVersionUID = 1L;', 'Works: name and regNo are restored, **phone = null** (the default of the new field)', 'The stream records the class\'s serialVersionUID; the reader compares it with the local class. Equal numbers mean “same class, compatible change”: fields missing in the stream get default values, extra fields in the stream are ignored.'],
    ['no serialVersionUID (the compiler computes it)', '**InvalidClassException**: “local class incompatible: stream classdesc serialVersionUID = …, local class serialVersionUID = …” (numbers differ)', 'The computed number is derived from the class structure (name, fields, methods); adding a field changes it, so the new class is judged incompatible with the old files and **all saved data becomes unreadable**.'],
  ], [2700, 3000, 3326], { leftAll: true, size: 16 }),
  caption('Table A4.2 – Effect of serialVersionUID (executed)'),
  P('**Rule:** always declare serialVersionUID explicitly; keep it when the change is compatible (adding a field, adding a method); change it, or write a custom readObject that migrates the data, when the change is incompatible (removing a field, changing a field\'s type, changing the class hierarchy).'),
  H2('Alternative Concept and Complexity Analysis – CSV or JSON text instead of serialization'),
  P('**The alternative solution.** Each resident becomes one text line (CSV: name,regNo,room,phone with a header line; quotes around a field that contains a comma) or one JSON object. The program writes the fields itself and parses them itself, so the format is independent of the class: it can be read by another language, opened in a text editor, and does not depend on serialVersionUID. The experiment FormatBenchmark saves and loads n residents in all three formats (round trip verified by the program).'),
  table(['n', 'Format', 'File size (bytes)', 'Save (ms)', 'Load (ms)'], fmts.map(r => [fmt(r.n), r.format, fmt(r.file_bytes), f1(r.save_ms), f1(r.load_ms)]), [1300, 2200, 2200, 1700, 1626], { size: 16 }),
  caption('Table A4.3 – Size and time (best of 5 runs; from TaskA4/format_results.csv)'),
  ...fig('graphs/a4_formats.png', 560, 220, 'Figure A4.2 – Size and times for 100,000 residents'),
  ...outFile('TaskA4/format_output.txt', 'Output of java FormatBenchmark (includes the new-field experiment)'),
  P(`**Analysis.** For 100,000 residents: serialization ${fmt(fm(100000, 'serialization').file_bytes)} bytes, CSV ${fmt(fm(100000, 'CSV').file_bytes)} bytes, JSON ${fmt(fm(100000, 'JSON').file_bytes)} bytes. CSV is the smallest (it has no field names) and the fastest to load (${f1(fm(100000, 'CSV').load_ms)} ms); JSON is the largest because every record repeats the field names; serialization is in between in size but slowest to load (${f1(fm(100000, 'serialization').load_ms)} ms) because the stream describes classes and objects and the reader rebuilds every object by reflection. All three are O(n) in time and space. **New field:** serialization copes if serialVersionUID is fixed (new field = default) and fails otherwise; a CSV reader that reads columns by position fails (ArrayIndexOutOfBoundsException in the experiment) unless it reads the header and looks columns up by name; JSON copes best because the reader looks fields up by key and a missing key can take a default. **When to choose:** serialization for a quick save of a complete object graph inside one program version; CSV/JSON when the data must outlive the program, be edited by people or be read by other systems.`),

  // ------------------------------------------------------------------ A5
  H1('Task A5 – Design for Correctness and Safety (4 marks, K4)'),
  H2('A5.1 – Transaction design (2 marks)'),
  ...code(`
ALGORITHM TransferMoney
INPUT   : fromAccount (hostel wallet), toAccount (canteen wallet), amount = 500
OUTPUT  : both balances changed, or neither changed
BEGIN
  1.  OPEN a connection;  SET oldMode <- con.getAutoCommit()
  2.  CALL con.setAutoCommit(false)                         // from now on, changes are NOT permanent until commit()
  3.  TRY
  4.      SET n1 <- executeUpdate("UPDATE account SET balance = balance - ? WHERE acc_id = ?", amount, fromAccount)
  5.      SET n2 <- executeUpdate("UPDATE account SET balance = balance + ? WHERE acc_id = ?", amount, toAccount)
  6.      IF n1 <> 1 OR n2 <> 1 THEN  THROW "account not found"  ENDIF
  7.      CALL con.commit()                                  // both updates become permanent together
  8.  CATCH any exception (SQLException, constraint violation CHECK balance >= 0, program error)
  9.      CALL con.rollback()                                // undo the first update: neither change is kept
  10.     RETURN failure
  11. FINALLY
  12.     CALL con.setAutoCommit(oldMode);  CLOSE the connection
  13. RETURN success
END`),
  gap(),
  ...fig('diagrams/a5_transaction.png', 570, 300, 'Figure A5.1 – One transaction against two auto-committed updates'),
  P('**Why money is never lost or created.** A transaction is atomic: the two updates are one unit. If the program fails between them (exception, crash, power failure), the first update was never committed, so the database discards it (rollback, or recovery after a crash) and both balances are as before. The UPDATE uses balance = balance − ? instead of reading the balance and writing a computed value, so two simultaneous transfers cannot overwrite each other\'s result; the CHECK (balance ≥ 0) constraint in the table rejects a debit that would make the balance negative, and the rejection rolls the whole transfer back.'),
  H2('A5.2 – Injection analysis (2 marks)'),
  P('The login check is  String sql = "SELECT * FROM users WHERE id=\'" + id + "\' AND pwd=\'" + pwd + "\'";'),
  ...fig('diagrams/a5_injection.png', 570, 280, 'Figure A5.2 – Concatenation lets the input change the SQL; PreparedStatement keeps input as data'),
  table(['Crafted input', 'SQL that is built', 'Why the query succeeds without a valid password'], [
    ['id = x,  pwd = \' OR \'1\'=\'1', 'SELECT * FROM users WHERE id=\'x\' AND pwd=\'\' OR \'1\'=\'1\'', 'AND binds tighter than OR, so the condition is (id=\'x\' AND pwd=\'\') OR \'1\'=\'1\'; \'1\'=\'1\' is always true, so **every row** matches.'],
    ['id = arun\' OR \'1\'=\'1,  pwd = anything', '... WHERE id=\'arun\' OR \'1\'=\'1\' AND pwd=\'anything\'', 'id=\'arun\' is true, so the password test is bypassed: the attacker is logged in as arun.'],
    ['id = admin\' --  (with a space after the dashes),  pwd = anything', 'SELECT * FROM users WHERE id=\'admin\' -- \' AND pwd=\'anything\'', '-- starts a comment: the password test is removed from the statement; the attacker is logged in as admin.'],
  ], [2400, 3100, 3526], { leftAll: true, size: 15 }),
  caption('Table A5.2 – Three crafted inputs (all three were executed in Task B2 and succeeded against the unsafe code)'),
  P('**Root cause.** The program builds code (SQL) by pasting data into it, so the database cannot tell where the programmer\'s SQL ends and the user\'s text begins. **Redesign:**'),
  ...code(`
ALGORITHM CheckLogin
INPUT   : id, pwd (text typed by the user)
OUTPUT  : true if exactly the matching user exists
BEGIN
  1.  PREPARE statement from the FIXED text  "SELECT * FROM users WHERE id = ? AND pwd = ?"   // the SQL structure is complete before any user data exists
  2.  CALL statement.setString(1, id)                                                         // id is sent as a value, never parsed as SQL
  3.  CALL statement.setString(2, pwd)
  4.  EXECUTE the query;  SET found <- the result has a row
  5.  CLOSE the result and the statement                                                      // try-with-resources
  6.  RETURN found
END`),
  gap(),
  P('With the PreparedStatement the three inputs above are compared as ordinary text values: no row has the id “admin\' -- ”, so the query returns no row (verified in Task B2). (Further protection, outside this question: store a salted hash of the password instead of the password, give the database user only the rights it needs, and do not show database error messages to the user.)'),
  H2('Alternative Concept and Complexity Analysis – two separate auto-commit updates'),
  P('**The alternative solution.** The two UPDATE statements are executed with auto-commit ON, so each is its own transaction and is made permanent immediately (TransferDemo, method transferAuto). The executed outcomes:'),
  ...outFile('TaskA5/transfer_output.txt', 'Output of java TransferDemo'),
);
{
  const tr = rd('TaskA5/transfer_output.txt');
  const m1 = tr.match(/one transaction per transfer \(1 commit\)\s+: ([0-9.]+) ms in total, ([0-9]+) us per transfer/), m2 = tr.match(/two auto-committed updates \(2 commits\)\s+: ([0-9.]+) ms in total, ([0-9]+) us per transfer/), m3 = tr.match(/ratio auto-commit \/ transaction\s+: ([0-9.]+)/);
  add(
    table(['Scenario (Rs.300 transfer; balances hostel / canteen)', 'Result', 'Total of both accounts'], [
      ['Transaction, no failure', '700 / 350', '1050 (unchanged)'],
      ['**Transaction**, crash between the two updates', '1000 / 50 (rolled back)', '1050 (unchanged)'],
      ['**Auto-commit**, crash between the two updates (debit first)', '700 / 50: the hostel wallet is debited, the canteen wallet never credited', '**750: Rs.300 LOST**'],
      ['**Auto-commit**, credit first, then the debit fails (balance too low)', '1000 / 2050: the canteen wallet was credited, the debit was rejected', '**3050: Rs.2000 CREATED**'],
      ['Transaction, credit first, then the debit fails', '1000 / 50 (rolled back)', '1050 (unchanged)'],
    ], [4000, 3200, 1826], { leftAll: true, size: 16 }),
    caption('Table A5.1 – Outcome of a failure between the two updates (executed)'),
    P(`**Time cost** (2,000 transfers, database on disk, every commit made durable): one transaction per transfer ${m1[2]} µs; two auto-committed updates ${m2[2]} µs; the ratio is ${m3[1]}. The reason is the number of commits, not the number of updates: a transaction commits once (one durable write), two auto-commit statements commit twice (about 0.8 ms each on this disk, see Task B3). So the **transaction is also faster**, not slower. The cost of a transaction is something else: it holds its locks until commit, which matters under heavy concurrency, but for two short updates this is negligible. **Risk to data correctness:** auto-commit is correct only when each statement is independently meaningful; for any operation made of several statements that must succeed together (transfers, importing a file, issuing a pass and updating a counter) auto-commit leaves a window in which a failure produces lost or invented money, as the table shows. Space cost: O(1) in both designs (the transaction keeps the uncommitted changes of two rows in the log).`),
  );
}

// =====================================================================================================
// PART B
// =====================================================================================================
const ratio = (a, b) => Number(a) / Number(b);
add(
  H1('PART B – Implementation & Experimental Investigation (25 marks)'),
  P('Part B implements the designs of Part A, tests them, measures them and interprets the results. All Java programs listed here were compiled and executed; the console outputs are pasted unchanged. The JDBC programs ran against the real SQLite engine through the test driver described in the front matter and Appendix B (see the limitations there).'),

  // ------------------------------------------------------------------ B1
  H1('Task B1 – Implement and Instrument the File Store (7 marks, K4)'),
  P('The bus-pass store designed in A1 (text format) and the line counters of A2 were implemented in Java. The classes are: BusPass (record, text line, parsing), DamagedRecordException, BusPassStore (add / find / listByRoute), LineCounter (the counters of A2), FixedRecordStore (the RandomAccessFile alternative), PassStore (the Complete-the-Code class) and the test and benchmark programs. All source code follows in this task.'),
  H2('B1.1 – Validate the implementation (2 marks)'),
  P('BusPassStoreTest runs eleven tests; each is listed with what it verifies. The damaged lines of tests T7–T10 were created on purpose by appending bad text to the store file.'),
  ...outFile('TaskB1/buspassstore_test_output.txt', 'Output of java BusPassStoreTest'),
  table(['Test', 'Case', 'What it verifies'], [
    ['T0', 'find() on a missing file', 'A missing file is an empty store, not an exception (A1.3).'],
    ['T1', 'add four passes, find P1002', 'Normal operation: record written and read back with all fields equal.'],
    ['T2', 'name “Kumar, Meena”', 'Delimiter inside a field is escaped on writing and restored on reading (A1.1); the stored line shows Kumar\\, Meena.'],
    ['T3', 'name with a backslash', 'The escape character itself is escaped correctly.'],
    ['T4', 'list route 12', 'listByRoute returns exactly the two passes of that route.'],
    ['T5', 'find(“P9999”)', 'An id that does not exist returns null, with no exception.'],
    ['T6', 'duplicate pass id', 'add() rejects a second pass with an existing id.'],
    ['T7', 'five bad lines, then a good one', 'Damaged lines do not stop the search: P2000 is still found after them.'],
    ['T8', 'full scan after the damage', 'Exactly five damaged lines are reported with line number and reason: wrong field count, bad number (checksum correct but amount not numeric), empty line, changed text (checksum mismatch), cut-off last line.'],
    ['T9', 'good record before the damage', 'Earlier records are not affected by later damage.'],
    ['T10', 'add after a cut-off last line', 'The missing line break is repaired first, so the new record is not glued to the broken one (A1.3).'],
  ], [700, 2700, 5626], { leftAll: true, size: 16 }),
  caption('Table B1.1 – What each test verifies. Result: 11 passed, 0 failed.'),
  P('One test of my own first failed because of a flaw in the test, not in the store: find() stops at the first match, so the cut-off last line after P2000 had not been scanned when I counted the damaged lines. T8 now uses a full scan (listByRoute) to count all damaged lines; this is a property of the design worth stating: the damaged-line report covers only the part of the file that was scanned.'),
  H2('B1.2 – Compare with the Part A pseudocode (1 mark)'),
  table(['Part A pseudocode', 'What I had to change in the implementation', 'Why'], [
    ['Parse: “split at unescaped commas”', 'Wrote a small tokenizer (BusPass.split) instead of String.split(",")', 'String.split cannot tell an escaped comma from a delimiter; with split a name such as “Kumar\\, Meena” would give nine fields.'],
    ['Order of checks in Parse()', 'Field count → CRC → number / date conversion', 'The CRC is computed on the text before the last unescaped comma, so the field count has to be known first. A consequence: a hand-edited bad amount is reported as “checksum mismatch” unless the checksum was recomputed (test T8 uses a line with a correct checksum to reach the “not a number” message).'],
    ['AddPass step 2: duplicate check', 'Added a second method append() without the check', 'The check is an O(n) scan, so loading n records with add() would cost O(n²); benchmarks and the migration load with append().'],
    ['AddPass step 5: repair the missing line break', 'Implemented with RandomAccessFile (seek to the last byte)', 'readLine() of BufferedReader cannot tell whether the final line ended with a line break; checking the last byte is O(1).'],
    ['FindPass: report damaged lines', 'The report is a list in the store object, cleared at the start of every scan', 'Because find() returns early, only the scanned part of the file is reported (see B1.1).'],
    ['“PassStore” of the assignment (5 fields, String.split, no checksum)', 'Kept exactly as given for the Complete-the-Code task; the full 8-field design is BusPassStore', 'The Complete-the-Code class is a simplified version of the store: it has no escaping and no checksum, so a name with a comma would be lost (shown in Task B2).'],
  ], [2400, 3000, 3626], { leftAll: true, size: 15 }),
  caption('Table B1.2 – Differences between design and implementation'),

  H2('B1.3 – Measure the effect of buffering (2 marks)'),
  P('BufferingBenchmark counts the lines of attendance logs of 1, 10 and 50 MB (33-byte lines) with five methods. “Read calls” counts the calls that reach the FileInputStream, which for a FileInputStream is one operating-system read each. Times are the best of 5 runs (best of 3 for the unbuffered 1 and 10 MB, a single run for the unbuffered 50 MB).'),
  table(['Size (MB)', 'Method', 'Time (ms)', 'Read calls', 'File bytes'], buf.map(r => [r.size_MB, r.method, f1(r.time_ms), Number(r.read_calls) < 0 ? '(not counted)' : fmt(r.read_calls), fmt(r.file_bytes)]), [1100, 2900, 1500, 2000, 1526], { size: 15 }),
  caption('Table B1.3 – Line counting with and without buffering (from TaskB1/buffering_results.csv)'),
  ...fig('graphs/b1_buffering.png', 560, 230, 'Figure B1.1 – Time against file size (left) and read calls for the 50 MB file (right)'),
  ...outFile('TaskB1/buffering_output.txt', 'Output of java BufferingBenchmark'),
  P(`**Comparison with the prediction of A2.1.** (1) Number of calls: predicted file size + 1; measured for the unbuffered counter: ${['1', '10', '50'].map(s => { const r = buf.find(x => x.size_MB === s && x.method === 'unbuffered read()'); return `${s} MB: ${fmt(r.read_calls)} = ${fmt(r.file_bytes)} + ${Number(r.read_calls) - Number(r.file_bytes)}`; }).join('; ')} – the prediction was exact. (2) Time: predicted about 10 s for 50 MB; measured ${f1(B50('unbuffered read()').time_ms / 1000)} s (${(B50('unbuffered read()').time_ms * 1e6 / B50('unbuffered read()').read_calls).toFixed(0)} ns per call): the right order of magnitude. (3) Effect of buffering: with a BufferedInputStream the calls fell by a factor of ${fmt(Math.round(B50('unbuffered read()').read_calls / B50('BufferedInputStream').read_calls))} and the time from ${f1(B50('unbuffered read()').time_ms / 1000)} s to ${f2(B50('BufferedInputStream').time_ms / 1000)} s (${Math.round(B50('unbuffered read()').time_ms / B50('BufferedInputStream').time_ms)} times faster); with a 64 KB array the calls fell by ${fmt(Math.round(B50('unbuffered read()').read_calls / B50('byte[64 KB] buffer').read_calls))} and the time to ${f1(B50('byte[64 KB] buffer').time_ms)} ms (${fmt(Math.round(B50('unbuffered read()').time_ms / B50('byte[64 KB] buffer').time_ms))} times faster). The time grows linearly with the file size in every method (O(file size)); buffering changes the constant, not the complexity. What surprised me: the BufferedInputStream with a one-byte read() is still ${Math.round(B50('BufferedInputStream').time_ms / B50('byte[64 KB] buffer').time_ms)} times slower than the own byte[] buffer, because 52 million method calls remain.`),

  H2('B1.4 – Measure how the cost grows (2 marks)'),
  P('GrowthBenchmark creates stores of 1,000 to 100,000 passes and measures one find() for the last pass (worst case that exists), for a pass in the middle, and for an id that is not in the file (the scan must read everything). It also measures the fixed-length RandomAccessFile store and a HashMap loaded once. Averages of 10–30 runs after a warm-up.'),
  table(['n', 'Text file bytes', 'Fixed file bytes', 'scan: last (ms)', 'scan: middle (ms)', 'scan: missing (ms)', 'fixed seek (µs)'], grow.map(r => [fmt(r.n), fmt(r.text_bytes), fmt(r.fixed_bytes), f2(r.scan_last_ms), f2(r.scan_middle_ms), f2(r.scan_missing_ms), f2(r.fixed_seek_us)]), [900, 1400, 1400, 1300, 1400, 1400, 1226], { size: 15 }),
  caption('Table B1.4 – Time of one find() as the file grows (from TaskB1/growth_results.csv)'),
  ...fig('graphs/b1_growth.png', 520, 330, 'Figure B1.2 – Growth of the time to find one pass (logarithmic axes)'),
  P(`**Interpretation.** The scan time grows with n: from n = 10,000 to 100,000 (×10) the time to find the last pass grew from ${f2(grow[2].scan_last_ms)} ms to ${f2(grow[4].scan_last_ms)} ms (×${(grow[4].scan_last_ms / grow[2].scan_last_ms).toFixed(1)}), and the middle pass is found in about half the time (the scan stops halfway) – this is O(n), as predicted in A1. Between 1,000 and 100,000 the time grew ×${(grow[4].scan_last_ms / grow[0].scan_last_ms).toFixed(0)} for ×100 records: less than 100 because the small runs include fixed costs (opening the file, JIT warm-up) that do not grow with n. The “missing” id costs about as much as the last pass because both read the whole file. The fixed-length store needs ${f1(grow[0].fixed_seek_us)}–${f1(Math.max(...grow.map(r => Number(r.fixed_seek_us))))} µs for every n (O(1)). For the stated need (8,000 passes a year) a scan takes about ${f1(grow[2].scan_last_ms)} ms per search at 10,000 passes, which a clerk does not notice; the trend shows when that stops being true.`),

  H2('Complete the Code – PassStore.find (Task B1)'),
  P('TODO 1 reads the file line by line with a BufferedReader inside try-with-resources, skips damaged lines (not exactly five fields) and returns the fields of the matching pass. Everything else of the program is as given.'),
  ...codeFile('TaskB1/PassStore.java', 'TODO 1 completed'),
  ...outFile('TaskB1/passstore_output.txt', 'Output of java PassStore (expected: the five fields of P1003, then null)'),
  P('**The output equals the expected output.** P1003 is found although a damaged line (P1002,DAMAGED LINE: two fields) comes before it, because a damaged line is skipped with continue and the loop goes on. find(“P1002”) returns null: the line with that id is damaged and therefore not a valid pass; the program does not guess. split(",", -1) is used so that trailing empty fields are kept (“a,b,” has three fields, not two), otherwise a line with a missing last field could be mistaken for a valid shorter line.'),

  H2('Alternative Concept and Complexity Analysis – HashMap loaded once'),
  ...code(`
ALGORITHM SearchManyWithHashMap
INPUT   : the pass file, q pass ids to look up
BEGIN
  1.  SET map <- empty HashMap from passId to pass
  2.  Scan the file ONCE and put every valid pass into the map                  // O(n) time, O(n) memory
  3.  FOR EACH id IN the q ids DO  SET pass <- map.get(id)  ENDFOR             // O(1) each
END
// the scanning version calls FindPass for every id: q scans of n lines = O(q * n)`),
  gap(),
  table(['q = 100 searches', 'n', 'Scan the file for every search (ms)', 'HashMap: load once + 100 lookups (ms)', 'Speed-up'], many.map(r => [r.q, fmt(r.n), f1(r.scan_total_ms), f1(r.hashmap_total_ms), (ratio(r.scan_total_ms, r.hashmap_total_ms)).toFixed(1) + '×']), [1500, 1300, 2400, 2700, 1126], { size: 16 }),
  caption('Table B1.5 – q searches on n passes (from TaskB1/search_many_results.csv)'),
  ...fig('graphs/b1_search_many.png', 420, 280, 'Figure B1.3 – Total time of 100 searches'),
  table(['n', 'HashMap load (ms)', 'HashMap lookup (ns)', 'Heap used by the map (MB)', 'Heap used by the scan'], grow.slice(1).map(r => [fmt(r.n), f1(r.map_load_ms), f1(r.map_lookup_ns), f1(r.map_heap_MB), 'O(1): one 8 KB buffer']), [900, 2000, 2000, 2400, 1726], { size: 16 }),
  caption('Table B1.6 – Cost of the HashMap alternative (the heap figure for n = 1,000 is lost in garbage-collection noise and is omitted)'),
  P(`**Analysis.** Scanning for every search costs O(q·n); the HashMap costs O(n + q): one scan plus q constant-time lookups. For n = 100,000 and q = 100 the total fell from ${f1(many[2].scan_total_ms)} ms to ${f1(many[2].hashmap_total_ms)} ms (${(ratio(many[2].scan_total_ms, many[2].hashmap_total_ms)).toFixed(0)}× faster), and the gain grows with q. The price is memory: O(n) – about ${f1(grow[4].map_heap_MB)} MB for 100,000 passes against a single 8 KB buffer for the scan – and the map is a snapshot: passes added later by another program are not seen until the file is read again. For one search the scan is cheaper (the map needs the whole file to be read first); the break-even is at about q = 1 here. **Evaluation:** a long-running program that answers many searches should load the file into a map (or use a database); a program that does one search and exits should scan.`),

  H3('Source code of Task B1'),
  ...codeFile('TaskB1/DamagedRecordException.java'), ...codeFile('TaskB1/BusPass.java', 'record, escaping, CRC, parsing'), ...codeFile('TaskB1/BusPassStore.java', 'add / find / listByRoute'),
  ...codeFile('TaskB1/LineCounter.java', 'the counters of A2'), ...codeFile('TaskB1/FixedRecordStore.java', 'the RandomAccessFile alternative'),
  ...codeFile('TaskB1/BusPassStoreTest.java'), ...codeFile('TaskB1/BufferingBenchmark.java'), ...codeFile('TaskB1/GrowthBenchmark.java'), ...codeFile('TaskB1/ReadAllLinesMemory.java'),

  // ------------------------------------------------------------------ B2
  H1('Task B2 – Engineer an Adversarial Input (5 marks, K5)'),
  P('B2 asks for deliberately constructed inputs that make the designs of Part A fail, and for the response of the repaired designs. The experiments use a small **local test database** (table users, created only for this assignment; the passwords are test values).'),
  ...codeFile('TaskB2/users.sql'), ...codeFile('TaskB2/LoginCheck.java', 'unsafe(), safe() and the two weaker fixes'),
  H2('B2.1 – Inputs that fool the unsafe login check'),
  P('AdversarialDemo runs each input against the original string-built query and against the PreparedStatement version. **None of the three attacks contains a valid password.**'),
  ...outFile('TaskB2/adversarial_output.txt', 'Output of java AdversarialDemo'),
  table(['Input (id / pwd)', 'SQL built by the unsafe code', 'Unsafe check', 'PreparedStatement'], [
    ['A1:  x  /  \' OR \'1\'=\'1', 'SELECT * FROM users WHERE id=\'x\' AND pwd=\'\' OR \'1\'=\'1\'', '**accepted, returns all four users**', 'rejected'],
    ['A2:  arun\' OR \'1\'=\'1  /  anything', '... WHERE id=\'arun\' OR \'1\'=\'1\' AND pwd=\'anything\'', '**accepted as arun**', 'rejected'],
    ['A3:  admin\' --   /  anything', '... WHERE id=\'admin\' -- \' AND pwd=\'anything\'', '**accepted as admin**', 'rejected'],
    ['normal: arun / secret123', 'a correct query', 'accepted', 'accepted'],
    ['normal: arun / wrongpass', 'a correct query', 'rejected', 'rejected'],
  ], [2300, 3000, 2000, 1726], { leftAll: true, size: 15 }),
  caption('Table B2.1 – Adversarial and normal inputs (executed): the two versions agree on normal input and differ on every attack'),
  H2('B2.2 – A damaged bus-pass file'),
  P('DamagedFileDemo builds a bus-pass file containing the damage named in the assignment (wrong number of fields, non-numeric amount, empty line) and six more kinds, between good records, and lists the response of BusPassStore:'),
  ...outFile('TaskB2/damagedfile_output.txt', 'Output of java DamagedFileDemo'),
  table(['Damage', 'Reported as', 'Good records'], [
    ['wrong number of fields (3 instead of 8)', 'expected 8 fields but found 3', 'unaffected'],
    ['name with an unescaped comma (nine fields)', 'expected 8 fields but found 9', 'unaffected'],
    ['non-numeric amount ("forty"), non-numeric route ("abc")', 'not a number: ...', 'unaffected'],
    ['impossible date 2027-13-45', 'bad validity date', 'unaffected'],
    ['negative amount', 'amount must be positive', 'unaffected'],
    ['empty line', 'empty line', 'unaffected'],
    ['line edited by hand / line cut off by a power failure', 'checksum mismatch / wrong field count', 'unaffected'],
  ], [3600, 3400, 2026], { leftAll: true, size: 16 }),
  caption('Table B2.3 – Response of the store to nine damaged lines: all nine are reported with a line number, and the three good records are still found'),
  H2('B2.3 – What each input reveals about the original designs'),
  table(['Input', 'What it reveals'], [
    ['A1  (pwd = \' OR \'1\'=\'1)', 'The query returns **every** row: the program has no check that exactly one user matched, and the attacker also learns that the database content is reachable. It reveals that data and code are mixed and that operator precedence (AND before OR) can be exploited.'],
    ['A2  (id = arun\' OR ...)', 'The password test can be skipped for a **chosen** account: injection is not only a data leak, it is an authentication bypass.'],
    ['A3  (id = admin\' -- )', 'A comment sequence **removes the rest of the query**; the attacker does not even need to know the structure of the password test.'],
    ['A legitimate password with an apostrophe (obrien / it\'s-me)', 'The unsafe code fails with an SQL syntax error for an honest user: the design is also a **correctness bug**, not only a security hole.'],
    ['Wrong number of fields in the file', 'The original 5-field store using String.split would silently ignore (or mis-read) the line: it reveals that nothing in the original design detects damage.'],
    ['Non-numeric amount ("forty")', 'The original would store it without validation and fail later in Double.parseDouble: it reveals missing input validation.'],
    ['Empty line / cut-off last line', 'The original has no rule for blank or half-written lines; a power failure during add could silently corrupt the next record.'],
    ['Name containing the delimiter', 'Without escaping the record splits into too many fields: it reveals the weakness of a plain delimiter format (A1.1).'],
  ], [3000, 6026], { leftAll: true, size: 16 }),
  caption('Table B2.4 – Interpretation of the inputs'),
  H2('Alternative Concept and Complexity Analysis – escaping the quotes, or validating with a regular expression'),
  P('**The alternative solutions.** (a) Keep the string-built SQL but double every apostrophe (replace("\'", "\'\'")). (b) Keep the string-built SQL but accept only ids and passwords that match [A-Za-z0-9_]{1,20}. Both are implemented in LoginCheck (escaped(), regex()).'),
  table(['Input', 'Escaping quotes', 'Regex validation', 'PreparedStatement'], [
    ['A1, A2, A3 (quote-based attacks)', 'rejected', 'rejected', 'rejected'],
    ['Numeric context: WHERE user_no = <input>, input “0 OR 1=1” (no quotes needed)', '**accepted: returns all four users** (SQL: ... WHERE user_no = 0 OR 1=1)', 'would reject (letters/spaces not allowed)', 'rejected (the value is a number or an error)'],
    ['Honest password with an apostrophe: it\'s-me', 'accepted (correct)', '**rejected: an honest user cannot log in**', 'accepted (correct)'],
    ['Honest password with # and !: Adm!n#2026', 'accepted', '**rejected**', 'accepted'],
  ], [3000, 2300, 2000, 1726], { leftAll: true, size: 15 }),
  caption('Table B2.5 – Weaknesses of the alternatives (executed, except the cell “would reject”, which follows from the pattern). Escaping fails whenever the input is not inside quotes; regex validation blocks honest input and has to be remembered in every query.'),
  P('**Why they are weaker.** Escaping depends on the database dialect and on the character set: MySQL also treats a backslash as an escape character in strings, so a naive “double the apostrophe” rule can be defeated by an input ending in a backslash (this MySQL-specific bypass could not be demonstrated here because SQLite does not treat the backslash specially; it is a documented limitation, not an executed result). It must be applied to every value in every query, and one forgotten value reopens the hole. A regular expression is a **whitelist of characters**, which blocks injection but also blocks honest users (apostrophes in names, # and ! in passwords) and does not protect numeric parameters unless the pattern is exactly right. PreparedStatement is different in kind: the SQL structure is fixed before the data arrives, so there is nothing to forget.'),
);
{
  const adv = rd('TaskB2/adversarial_output.txt');
  const lines = [...adv.matchAll(/^\s{3}(string-built Statement \(unsafe\)|escape quotes \+ Statement|regex validate \+ Statement|PreparedStatement, prepared every time|PreparedStatement, prepared ONCE)\s+([0-9.]+) us/gm)];
  const vn = adv.match(/validation work alone: regex match ([0-9]+) ns, quote escaping ([0-9]+) ns/);
  add(
    table(['Version (20,000 logins, best of 5)', 'Time per login (µs)'], lines.map(m => [m[1], m[2]]), [6000, 3026], { leftAll: false, size: 16 }),
    caption('Table B2.6 – Cost per login check (executed; database on disk, read-only queries)'),
    ...fig('graphs/b2_login_cost.png', 440, 240, 'Figure B2.1 – Cost of one login check'),
    P(`**Analysis.** The validation work itself is negligible: a regular-expression match takes about ${vn[1]} ns and quote escaping ${vn[2]} ns per input, i.e. ${(vn[1] / 1000).toFixed(2)} µs and ${(vn[2] / 1000).toFixed(2)} µs against about ${lines[0][2]} µs for the query. The real difference is how the statement reaches the database: a string-built Statement must be parsed again for every login (${lines[0][2]} µs), a PreparedStatement that is prepared once and reused is the fastest (${lines[4][2]} µs, about ${(lines[0][2] / lines[4][2]).toFixed(0)}× faster), and a PreparedStatement that is re-prepared for every call (${lines[3][2]} µs) costs roughly the same as the string-built one because preparing is the parse. All five are O(1) per login in time and space. **Evaluation:** security does not cost speed here: the safe version is not slower than the unsafe one, and when the statement is reused it is much faster. There is no performance argument for the weaker fixes.`),
    H3('Source code of Task B2'),
    ...codeFile('TaskB2/AdversarialDemo.java'), ...codeFile('TaskB2/DamagedFileDemo.java'),
  );
}

// =====================================================================================================
// B3
// =====================================================================================================
const lr = csv('TaskB3/loader_results.csv');
const LR = (d, n, a, m) => { const v = lr.filter(r => r.dir === d && Number(r.rows) === n && r.approach === a && r.mode === m).map(r => Number(r.ms)); return v.length ? Math.min(...v) : null; };
const APS = ['Statement', 'PreparedStatement', 'Prepared+Batch(1000)'];
const loaderRows = d => { const out = []; [1000, 10000, 100000].forEach(n => APS.forEach(a => { const au = LR(d, n, a, 'autocommit'), tx = LR(d, n, a, 'transaction'); out.push([fmt(n), a, au === null ? '–' : f1(au), tx === null ? '–' : f1(tx), au && tx ? (au / tx).toFixed(1) + '×' : '–', tx ? (tx * 1000 / n).toFixed(1) : '–']); })); return out; };
const fo = rd('TaskB3/failure_output.txt'); const left = [...fo.matchAll(/rows left in the table: ([0-9]+)/g)].map(m => m[1]);
const mr = [...rd('TaskB3/multirow_output.txt').matchAll(/^\s*([0-9,]+) (addBatch, batches of 1,000|multi-row INSERT, b = 100|multi-row INSERT, b = 500|PreparedStatement per row)\s+([0-9.]+)\s+([0-9.]+)\s+([0-9,]+)\s*$/gm)];
const sqlLen = rd('TaskB3/multirow_output.txt').match(/b = 500 rows: ([0-9]+) characters/);

add(
  H1('Task B3 – Statement vs PreparedStatement vs Batch (8 marks, K5)'),
  P('B3 compares three ways to insert passes into the table bus_pass, with auto-commit and with transactions, and completes the batch loader PassLoader. The table (MySQL script; the same text also runs in SQLite):'),
  ...codeFile('TaskB3/bus_pass.sql'),
  ...fig('diagrams/b3_loader_flow.png', 420, 440, 'Figure B3.1 – Control flow of PassLoader.loadAll() (TODO 2 and TODO 3 are the commit and rollback paths)'),
  H2('B3.1 – Correctness on small data (2 marks)'),
  P('B3Correctness inserts the same 10 records with (a) a string-built Statement, (b) a PreparedStatement and (c) PassLoader (batch + transaction), and for each reads the table back with SELECT, compares every field with the source value, and prints the column names and types from ResultSetMetaData.'),
  ...outFile('TaskB3/b3_correctness_output.txt', 'Output of java B3Correctness'),
  P('**Result: 3 of 3 pass.** Every version stored exactly 10 rows with all fields equal to the source. ResultSetMetaData confirms the table definition (pass_id VARCHAR(10), reg_no VARCHAR(12), name VARCHAR(60), route_no INT, amount DECIMAL(8,2)) and the matching java.sql.Types, so the program can check what it is reading instead of assuming. The completed PassLoader returned 10 and restored auto-commit to true.'),
  H2('B3.2 – Increase the data size (2 marks)'),
  P('LoaderBenchmark inserts 1,000, 10,000 and 100,000 generated passes into a fresh database file for every run, with each approach, in two commit modes. Table B3.1 is for a database on a real disk, where every commit is made durable (the realistic case); Table B3.2 repeats the runs with the database file in memory (/dev/shm) to show the cost of the work without disk waits.'),
  table(['Rows', 'Approach', 'Auto-commit (ms)', 'One transaction (ms)', 'Auto-commit ÷ transaction', 'Transaction: µs per row'], loaderRows('/tmp'), [900, 2500, 1500, 1700, 1500, 1426], { size: 15 }),
  caption('Table B3.1 – Insert time, database on disk (from TaskB3/loader_results.csv). Single runs for the large cases, best of 3 for the 1,000-row transaction cases.'),
  table(['Rows', 'Approach', 'Auto-commit (ms)', 'One transaction (ms)', 'Auto-commit ÷ transaction', 'Transaction: µs per row'], loaderRows('/dev/shm'), [900, 2500, 1500, 1700, 1500, 1426], { size: 15 }),
  caption('Table B3.2 – The same runs with the database file in memory (no disk waits)'),
  ...fig('graphs/b3_loader.png', 560, 300, 'Figure B3.2 – Insert time for the three approaches in both commit modes (logarithmic scale)'),
  ...outFile('TaskB3/loader_output_part2.txt', 'Excerpt of the benchmark output: 100,000 rows, one transaction, disk'),
  P(`**Interpretation – within the transaction (parse cost and call overhead).** For 100,000 rows in one transaction on disk: Statement ${f1(LR('/tmp', 100000, 'Statement', 'transaction'))} ms, PreparedStatement ${f1(LR('/tmp', 100000, 'PreparedStatement', 'transaction'))} ms, Prepared+Batch ${f1(LR('/tmp', 100000, 'Prepared+Batch(1000)', 'transaction'))} ms. The string-built Statement is slowest because the database parses 100,000 different SQL texts; the PreparedStatement parses once and only binds values (${(LR('/tmp', 100000, 'Statement', 'transaction') / LR('/tmp', 100000, 'PreparedStatement', 'transaction')).toFixed(1)}× faster); the batch is faster again (${(LR('/tmp', 100000, 'PreparedStatement', 'transaction') / LR('/tmp', 100000, 'Prepared+Batch(1000)', 'transaction')).toFixed(1)}×) because it also reduces the per-statement call overhead. All three grow linearly with the number of rows: O(n). **How reliable is the last comparison?** Not very: in the separate run of MultiRowBenchmark (Table B3.5) the plain PreparedStatement loop took 231 ms and the batch 311 ms for 100,000 rows, the opposite order. Differences of about 30% between PreparedStatement and batch are therefore within the run-to-run variation of this machine (JIT compilation, garbage collection, file-system state); the robust results are the two large effects: string-built Statement against PreparedStatement (the parse cost) and auto-commit against one transaction (two orders of magnitude, B3.3). **A caution about the batch:** with this test driver a batch is executed as a loop on the same machine, so no network round trip is saved; with a real MySQL server over a network, batching also removes one network round trip per row (and with rewriteBatchedStatements=true the driver rewrites the batch into multi-row inserts), so the advantage of the batch there is expected to be larger than measured here. I could not measure that.`),
  H2('B3.3 – Auto-commit against one transaction (2 marks)'),
  P(`**Speed.** The auto-commit rows in Table B3.1 show the dominant effect: with auto-commit every INSERT is its own transaction and must be made durable before the next one starts, which costs about 0.8 ms on this disk, **whichever approach is used** (100,000 rows: Statement ${fmt(Math.round(LR('/tmp', 100000, 'Statement', 'autocommit') / 1000))} s, PreparedStatement ${fmt(Math.round(LR('/tmp', 100000, 'PreparedStatement', 'autocommit') / 1000))} s, batch ${fmt(Math.round(LR('/tmp', 100000, 'Prepared+Batch(1000)', 'autocommit') / 1000))} s; all about 0.8 ms per row). Putting the same batch in one transaction made the load **${Math.round(LR('/tmp', 100000, 'Prepared+Batch(1000)', 'autocommit') / LR('/tmp', 100000, 'Prepared+Batch(1000)', 'transaction'))} times faster** (${f1(LR('/tmp', 100000, 'Prepared+Batch(1000)', 'autocommit') / 1000)} s → ${f2(LR('/tmp', 100000, 'Prepared+Batch(1000)', 'transaction') / 1000)} s), because one commit replaces 100,000. With the database in memory the difference shrinks to ${(LR('/dev/shm', 100000, 'Prepared+Batch(1000)', 'autocommit') / LR('/dev/shm', 100000, 'Prepared+Batch(1000)', 'transaction')).toFixed(0)}× (Table B3.2): the rest is the per-transaction overhead, which shows that most of the on-disk cost is waiting for the disk, not computing. This also means that the choice between Statement, PreparedStatement and batch **hardly matters when auto-commit is on**: the commit hides it.`),
  P('**Failure half-way.** FailureDemo makes the load fail at row 500 of 1,000, in two ways: (1) the amount of row 500 is the text “abc” (a program-level failure); (2) row 500 repeats the pass id of row 10 (the database rejects it). It then counts the rows that remain in the table.'),
  ...outFile('TaskB3/failure_output.txt', 'Output of java FailureDemo'),
  table(['Failure', 'PassLoader (one transaction)', 'Auto-commit, one PreparedStatement per row', 'Auto-commit, batches of 100'], [
    ['Row 500 has amount “abc” (NumberFormatException)', `**${left[0]} rows** (rolled back)`, `${left[1]} rows left behind`, `${left[2]} rows left behind`],
    ['Row 500 repeats a pass id (SQLIntegrityConstraintViolationException)', `**${left[3]} rows** (rolled back)`, `${left[4]} rows left behind`, `${left[5]} rows left behind`],
    ['No failure (control)', `${left[6]} rows`, '–', '–'],
  ], [3300, 2000, 2000, 1726], { leftAll: true, size: 16 }),
  caption('Table B3.3 – Rows remaining in bus_pass after a failure half-way'),
  P('**Meaning.** With the transaction the table is either completely loaded or untouched, so the import can simply be corrected and run again. Without it, the table holds a **partial load of unknown extent** (500 rows here); running the import again would fail with duplicate keys, and finding out which rows were loaded requires extra work. The completed PassLoader also restores the old auto-commit setting in a finally block, so a failure does not leave the connection in a different mode (printed in the output: “auto-commit mode afterwards: true”).'),
  H2('B3.4 – Engineering trade-off (2 marks)'),
  table(['Situation', 'Choice', 'Reason (evidence)'], [
    ['(a) A nightly import of 100,000 passes from a file', '**PreparedStatement with batches (1,000 rows) inside ONE transaction**, as in PassLoader', 'Measured: 0.3 s against 82 s for the same load with auto-commit (Table B3.1); all-or-nothing (Table B3.3), so a bad file cannot leave a half-loaded table; PreparedStatement also keeps the data out of the SQL text. If the MySQL server and the policy allow it, LOAD DATA INFILE is the usual fastest path (not measured here), but it needs file access on the server and its own error handling.'],
    ['(b) One pass issued at a counter', '**A single PreparedStatement INSERT with auto-commit on**', 'One statement is already one atomic transaction, so a transaction adds nothing; the ~0.8 ms durable commit is invisible to a clerk; a batch has nothing to batch. A PreparedStatement is still used for safety (B2). If issuing a pass involves several statements that must succeed together (pass row + payment record + counter), those statements need one transaction (A5).'],
  ], [2400, 2800, 3826], { leftAll: true, size: 16 }),
  caption('Table B3.4 – Choice for each situation'),
  H2('Complete the Code – PassLoader.loadAll (Task B3)'),
  P('The template was completed as follows: TODO 1 sets the five parameters (route as int, amount as double), adds the row to the batch and executes the batch after every 1,000 rows; TODO 2 commits and returns the number of rows; TODO 3 rolls back every insert of the transaction and re-throws the exception. (The template\'s SQL text was written on two lines; the missing + between the two string literals was restored.)'),
  ...codeFile('TaskB3/PassLoader.java', 'TODO 1, TODO 2 and TODO 3 completed'),
  P(`**Execution.** loadAll was run with 1,000, 10,000 and 100,000 passes in LoaderBenchmark (every run checks that the table holds exactly n rows afterwards): ${[1000, 10000, 100000].map(n => `${fmt(n)} rows in ${f1(LR('/tmp', n, 'Prepared+Batch(1000)', 'transaction'))} ms`).join('; ')} (database on disk). The invalid-amount case of FailureDemo (above) is the required test with an invalid amount in the middle of the list: the NumberFormatException is a RuntimeException, so it is caught by the second alternative of the catch clause, the rollback leaves **0 rows**, and the exception is re-thrown to the caller.`),
  H2('Alternative Concept and Complexity Analysis – one multi-row INSERT'),
  ...code(`
ALGORITHM InsertMultiRow
INPUT   : passes (n rows), b = rows per statement
BEGIN
  1.  SET sql <- "INSERT INTO bus_pass(...) VALUES " + b copies of "(?, ?, ?, ?, ?)" separated by commas
  2.  PREPARE statement from sql
  3.  FOR EACH group of b passes DO
  4.      bind 5 * b parameters;  EXECUTE the statement                  // one statement inserts b rows
  5.  ENDFOR
  6.  insert the remaining rows (fewer than b) with a statement prepared for that size
END
// LOAD DATA (MySQL):  LOAD DATA LOCAL INFILE 'passes.csv' INTO TABLE bus_pass FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';
// the server reads the whole file itself: no per-row JDBC call at all`),
  gap(),
  ...outFile('TaskB3/multirow_output.txt', 'Output of java MultiRowBenchmark (all runs in one transaction, database on disk)'),
  table(['Rows', 'Approach', 'Time (ms)', 'µs per row', 'Client bytes allocated per row'], mr.map(m => [m[1], m[2], m[3], m[4], m[5]]), [1100, 3300, 1500, 1500, 1626], { size: 16 }),
  caption('Table B3.5 – Multi-row INSERT against addBatch (from the output above)'),
  P(`**Analysis.** Time: all variants are O(n); a multi-row statement with b rows needs n/b statement executions instead of n, which saves the per-statement overhead. In this test the four variants differ by less than the run-to-run variation (at 100,000 rows: 231 ms for one PreparedStatement per row, 224–230 ms for the multi-row statements, 311 ms for addBatch; at 10,000 rows the order is different), so **no local speed advantage can be claimed** for batch or multi-row INSERT over a plain prepared loop, because the database and the client share one machine; across a network, n/b round trips against n/1,000 batched round trips would decide, and a larger b reduces them further. **Memory:** a statement with b rows is one SQL text of ${sqlLen ? fmt(sqlLen[1]) : '?'} characters for b = 500 (about ${sqlLen ? fmt(sqlLen[1] * 2) : '?'} bytes as a Java String) and 5·b = 2,500 parameter slots, i.e. O(b) memory for the statement itself, whereas addBatch keeps b parameter sets (O(b)) in the client until executeBatch; the "client bytes per row" column shows that both cost a few hundred bytes of garbage per row. The database also limits the size of one statement (MySQL: max_allowed_packet; SQLite: at most 32,766 parameters), so b must stay below it. **LOAD DATA** was not measured (not available in the test engine); it avoids all per-row work and is normally the fastest bulk path in MySQL, but it requires file access rights on the server or the client (security setting local_infile) and reports bad rows as warnings rather than exceptions, so the validation and failure report of B4 would have to be done separately. **Evaluation:** use batches in a transaction as the default; use multi-row INSERT or LOAD DATA only when the measured import time is not acceptable.`),
  H3('Source code of Task B3'),
  ...codeFile('TaskB3/PassData.java'), ...codeFile('TaskB3/PassInserters.java', 'Statement, PreparedStatement, batch, multi-row'), ...codeFile('TaskB3/Db.java'),
  ...codeFile('TaskB3/B3Correctness.java'), ...codeFile('TaskB3/LoaderBenchmark.java'), ...codeFile('TaskB3/FailureDemo.java'), ...codeFile('TaskB3/MultiRowBenchmark.java'),

  // ------------------------------------------------------------------ B4
  H1('Task B4 – Final Engineering Decision (5 marks, K5)'),
  P('The transport office will serve **12 campuses, about 100 times more passes (800,000), and several counters issuing passes at the same time**. B4 migrates the existing file, inspects the database, and decides what the production system should use.'),
  ...fig('diagrams/er_b4.png', 540, 330, 'Figure B4.1 – ER diagram of the production database (bus passes, campuses and the counter log)'),
  ...codeFile('TaskB4/bus_pass_full.sql'), ...codeFile('TaskB4/bus_pass_indexes.sql'), ...codeFile('TaskB4/issued_mysql.sql'),
  H2('B4.1 – A practical migration strategy (2 marks)'),
  P('**Strategy.** The migration reads the existing file line by line (BufferedReader), validates each line with the same rules as the file store (field count, CRC, numbers, date), rejects a line that is too long for its column or repeats a pass id already loaded (a duplicate would violate the PRIMARY KEY and abort the whole load), and inserts the good records with a PreparedStatement in batches of 1,000 **inside one transaction**: any unexpected failure rolls everything back, so the database is never half-loaded (B3.3). Rejected lines are collected with their line number and reason in a **failure report**, and the program finishes by verifying that the database holds exactly the accepted records (row count, number of distinct ids and the sum of the amounts).'),
  P('MigrationDemo builds an “existing” file of 8,000 good passes with 14 deliberately bad lines (wrong field count, non-numeric amount and route, impossible date, empty line, hand-edited line, negative amount, name too long for the column, an unescaped comma, a cut-off line, and four repeated pass ids) and migrates it:'),
  ...outFile('TaskB4/migration_output.txt', 'Output of java MigrationDemo'),
  P('**Result:** 8,014 lines read, 8,000 loaded, 14 rejected, each with its reason; the verification confirmed that the database contains exactly the 8,000 accepted passes (same count, same number of distinct ids, same sum of amounts), so the file and the database agree. The failure report is grouped by reason (for the operator) and listed line by line (for correcting the file).'),
  H2('B4.2 – Inspect the database (1 mark)'),
  P('DbInspector uses DatabaseMetaData to print the database product and version, the driver name and version, and the tables that exist (see the two reports at the beginning and at the end of the migration output above: the table bus_pass_full appears after it has been created).'),
  P('**Why this is useful.** (1) Portability and diagnostics: the program can log exactly which database and driver version it is talking to; most production problems (“works on my machine”) are version problems, and a support engineer needs this line first. (2) Self-checks: the program can verify at start-up that the expected tables exist before it tries to migrate or issue passes, and refuse to run (or create them) with a clear message instead of failing in the middle of a transaction. (3) Adapting to the database: behaviour that differs between products (limits, supported features) can be selected from the reported product instead of hard-coded. (4) Generic tools: a report generator or an export function can discover tables and columns (with getColumns and ResultSetMetaData) without knowing the schema. With MySQL the first two lines would read “MySQL” and its version, and the driver line “MySQL Connector/J”; here they read SQLite and the test driver (see the limitations).'),
  H2('B4.3 – The production decision (2 marks)'),
  P('ScaleBenchmark generates **800,000 passes** (100 × 8,000) and measures the three candidate designs: (A) one text file, (B) one serialized file per campus (12 files), (C) one database table with indexes. The concurrency question is investigated by ConcurrencyDemo.'),
  ...outFile('TaskB4/scale_output.txt', 'Output of java ScaleBenchmark'),
);
{
  const sc = csv('TaskB4/scale_results.csv'); const G = (d, m) => Number(sc.find(r => r.design === d && r.measure === m).value);
  const so = rd('TaskB4/scale_output.txt');
  const bLoad = so.match(/LOAD all 12 files ([0-9]+) ms \| heap needed ([0-9]+) MB/), bWrite = so.match(/write all ([0-9]+) ms/), cLoad = so.match(/load all rows \(batches of 1,000, one transaction\) ([0-9]+) ms/);
  const idx = so.match(/with an extra HashMap index: build ([0-9]+) ms, \+([0-9]+) MB heap, then ([0-9]+) ns per lookup/);
  const reg = so.match(/by reg_no WITHOUT index ([0-9.]+) ms \| by reg_no WITH index ([0-9.]+) ms/);
  const cc = rd('TaskB4/concurrency_output.txt');
  const conc = [...cc.matchAll(/^(\(\d\)[^\n]*)\n\s+(?:lines|passes issued): ([0-9]+)[^\n]*?(?:distinct pass numbers|distinct numbers): ([0-9]+) \| duplicate[^:]*: ([0-9]+)/gm)];
  add(
    table(['Design (800,000 passes)', 'Find ONE pass', 'List ONE route (20,000 passes)', 'Size on disk', 'Setup cost'], [
      ['(A) one text file', `${f1(G('text file', 'find one pass'))} ms (scan)`, `${f1(G('text file', 'list one route'))} ms (scan)`, `${fmt(G('text file', 'size'))} bytes`, 'none; but every search reads the whole file'],
      ['(B) 12 serialized files (all loaded into memory)', `${f1(G('per-campus serialization', 'search all for one id'))} ms (scan of all lists in memory); ${idx[3]} ns with an extra HashMap`, `${f1(G('per-campus serialization', 'list one route'))} ms`, `${fmt(G('per-campus serialization', 'size'))} bytes`, `load all files ${fmt(bLoad[1])} ms; **${bLoad[2]} MB of heap** (plus ${idx[2]} MB for the map)`],
      ['(C) database with indexes', `**${f2(G('database', 'find by primary key'))} ms** (primary key); by student number ${reg[1]} ms without an index, **${reg[2]} ms** with one`, `${f1(G('database', 'list one route'))} ms`, `${fmt(G('database', 'size'))} bytes (before the indexes)`, `load ${fmt(cLoad[1])} ms once; nothing to load afterwards`],
    ], [2200, 2400, 1800, 1300, 1326], { leftAll: true, size: 14 }),
    caption('Table B4.1 – Measured behaviour at the new scale (from TaskB4/scale_results.csv and the output above)'),
    ...fig('graphs/b4_scale.png', 500, 320, 'Figure B4.2 – Search time at 800,000 passes (logarithmic axis)'),
    P('**Concurrency.** Four counters issue 500 passes each at the same moment; every pass must get a unique number and no record may be damaged.'),
    ...outFile('TaskB4/concurrency_output.txt', 'Output of java ConcurrencyDemo'),
    table(['How the number is created', 'Passes issued', 'Distinct numbers', 'Duplicate numbers'], conc.map(m => [m[1].replace(/\s*\(run \d\)/, m[1].match(/run \d/) ? ' – ' + m[1].match(/run \d/)[0] : '').replace(/^\(\d\) /, ''), fmt(m[2]), fmt(m[3]), m[4]]), [4400, 1500, 1600, 1526], { leftAll: true, size: 15 }),
    caption('Table B4.2 – Four counters, 2,000 passes (executed)'),
    P(`**Decision: a relational database (MySQL) with an index on the primary key and on reg_no and route_no, accessed through PreparedStatement, with one transaction per issue and the bulk loads of B3/B4.1.** The evidence:`),
    B(`Search at scale: finding one pass in the text file takes ${f1(G('text file', 'find one pass'))} ms and grows with every pass (O(n)); the database needs ${f2(G('database', 'find by primary key'))} ms by primary key (O(log n)), i.e. ${fmt(Math.round(G('text file', 'find one pass') / G('database', 'find by primary key')))} times less, and a lookup by student number fell from ${reg[1]} ms to ${reg[2]} ms when the index was added. A clerk can tolerate 0.8 s once, but twelve campuses searching all day cannot.`),
    B(`Memory: design (B) must load the data of all campuses into the program (${bLoad[2]} MB here, growing with the passes); the database keeps the data on disk and returns only the rows asked for.`),
    B('Concurrency: without coordination the text file gave **duplicate pass numbers** (the counters read the same line count and wrote the same number; the count differs from run to run, which is exactly why such a defect passes testing). A lock inside one program (synchronized) removed the duplicates, but it protects only counters that are threads of the same program; 12 campuses are separate machines and programs. The database assigns the number itself (AUTO_INCREMENT) and gave 2,000 distinct numbers and no damaged rows, with each counter on its own connection.'),
    B('Correctness and safety: transactions (B3) make an issue all-or-nothing, constraints (PRIMARY KEY, NOT NULL, CHECK) reject bad data that the file store had to detect with a checksum, and PreparedStatement (B2) removes the injection risk.'),
    B('Evolution: a new column is an ALTER TABLE and not a change of file format for twelve programs (A4).'),
    P('**Costs of this decision (and when it would be wrong):** a database server has to be installed, backed up and secured; every counter needs network access to it; the team needs SQL knowledge; a power cut of the server stops all campuses. For a single counter with fewer than about 10,000 passes (the original situation, 1.5 ms per search in Table B1.4) the text file is still entirely adequate and simpler. The text export remains useful as a backup and as the migration source.'),
    H2('Alternative Concept and Complexity Analysis – one serialized file per campus'),
    P('**The alternative solution.** Each campus saves its List<BusPass> with ObjectOutputStream into its own file and loads it at start-up (design B above). Cross-campus questions load all twelve files.'),
    ...code(`
ALGORITHM SearchAllCampuses
INPUT   : passId
BEGIN
  1.  FOR campus FROM 1 TO 12 DO
  2.      IF the list of that campus is not in memory THEN  READ the campus file with ObjectInputStream  ENDIF      // O(n_c) time and memory
  3.      FOR EACH pass IN the list DO  IF pass.passId = passId THEN RETURN pass  ENDIF  ENDFOR                  // O(n_c)
  4.  ENDFOR
  5.  RETURN "not found"                                                                                        // O(N) in total, N = 800,000
END
ALGORITHM SavePass:  append the pass to the list in memory, then WRITE THE WHOLE LIST again with writeObject      // O(n_c) per change`),
    gap(),
    table(['', '(B) Serialized file per campus', '(C) Database'], [
      ['Search for one pass (N = 800,000)', `O(N) scan of the lists: ${f1(G('per-campus serialization', 'search all for one id'))} ms in memory; O(1) only with an extra HashMap (${idx[3]} ns, but +${idx[2]} MB and a build time of ${idx[1]} ms)`, `O(log N): ${f2(G('database', 'find by primary key'))} ms by primary key`],
      ['Start-up', `load 12 files: ${fmt(bLoad[1])} ms (O(N))`, 'none (connect only)'],
      ['Memory', `O(N): ${bLoad[2]} MB heap for 800,000 passes`, 'O(1) per query (the server holds a cache)'],
      ['Save one new pass', 'rewrite the whole campus file: O(n_c) (writing all 12 files took ' + fmt(bWrite[1]) + ' ms)', 'one INSERT: O(log N)'],
      ['Several counters of one campus at the same time', 'each counter has its own copy in memory; two counters writing the file overwrite each other\'s passes (**lost updates**) unless one program serializes all access', 'handled by the database (row locks, transactions)'],
      ['Counters of different campuses', 'independent files, so no conflicts between campuses, but no shared view: a pass issued at campus 3 is invisible to campus 5 until files are exchanged', 'one shared, consistent view'],
      ['A damaged file', 'a damaged serialized file is unreadable as a whole (one bad byte can lose all of that campus\'s passes)', 'the database recovers committed transactions after a crash'],
      ['Evolving the class', 'old files break unless serialVersionUID is managed (A4.2)', 'ALTER TABLE'],
    ], [2400, 3700, 2926], { leftAll: true, size: 15 }),
    caption('Table B4.3 – Serialized files per campus against the database (measured values from Table B4.1 and ConcurrencyDemo)'),
    P('**Conclusion.** Per-campus serialization works only if each campus has **one** program that owns its file and the questions never cross campuses. For the stated requirements (12 campuses, several counters at once, searches by pass id, student and route) the database is the only one of the three designs that satisfies all of them at the measured scale. The measurements also give the limits of the other designs: the single text file stops being adequate at roughly 100,000 passes (about 100 ms per search); per-campus serialization costs a heap proportional to the whole organisation and loses updates as soon as two counters share a campus.'),
    H3('Source code of Task B4'),
    ...codeFile('TaskB4/MigrationReport.java'), ...codeFile('TaskB4/Migration.java', 'validation + batches + one transaction + failure report'), ...codeFile('TaskB4/DbInspector.java'),
    ...codeFile('TaskB4/MigrationDemo.java'), ...codeFile('TaskB4/ScaleBenchmark.java'), ...codeFile('TaskB4/ConcurrencyDemo.java'),
  );
}

// =====================================================================================================
// APPENDICES
// =====================================================================================================
add(
  H1('Appendix A – Repository Layout, How to Run, GitHub Steps'),
  ...code(`
CSA09-<RegisterNumber>-Assignment5/
  README.md   GITHUB_STEPS.md
  TaskA1/  design notes (layouts, pseudocode are in the report)        TaskA2/  design notes
  TaskA3/  Dao, AbstractJdbcDao, StudentDao, CourseDao, MarksDao, scattered-SQL classes, A3Demo, student.sql, output
  TaskA4/  Resident, Hostel, Room, SerializationDemo, v1/ v2/ versioning.sh, FormatBenchmark, outputs, CSV
  TaskA5/  TransferDemo, wallet.sql, output
  TaskB1/  BusPass, BusPassStore, PassStore (Complete the Code), LineCounter, FixedRecordStore, tests, benchmarks, outputs, CSV
  TaskB2/  LoginCheck, AdversarialDemo, DamagedFileDemo, users.sql, outputs
  TaskB3/  PassLoader (Complete the Code), PassInserters, benchmarks, FailureDemo, bus_pass.sql, outputs, CSV
  TaskB4/  Migration, DbInspector, ScaleBenchmark, ConcurrencyDemo, SQL scripts, outputs, CSV
  diagrams/ graphs/ screenshots/ scripts/ tools/ report/`),
  gap(),
  H2('How to run the programs'),
  P('**With the test driver (no MySQL needed, Java 21):** from the repository root, for example  tools/run_jdbc.sh TaskB3 LoaderBenchmark /tmp 1000,10000 both loader_results.csv  (the script compiles the task folder together with the test driver and runs the class with --enable-preview --enable-native-access=ALL-UNNAMED).'),
  P('**With MySQL (final submission):** (1) install MySQL and create a database; run the .sql scripts of the task (bus_pass.sql, users.sql, student.sql, wallet.sql, bus_pass_full.sql); (2) download MySQL Connector/J and put the jar on the class path; (3) in the program, remove the line  minisqlite.MiniSqlite.install();  and replace the URL  "jdbc:minisqlite:" + file  by  "jdbc:mysql://localhost:3306/<database>"  with your user name and password (the files Db.java, AdversarialDemo.java, A3Demo.java, TransferDemo.java, MigrationDemo.java, ScaleBenchmark.java and ConcurrencyDemo.java contain the URL); (4) compile and run with  javac -cp .:mysql-connector-j-8.x.jar *.java  and  java -cp .:mysql-connector-j-8.x.jar <MainClass>  (use ; instead of : on Windows). The JDBC code uses only java.sql interfaces, so nothing else changes; remember that MySQL needs  -- (dash dash space)  for SQL comments and that INTEGER PRIMARY KEY in the test table issued is INT AUTO_INCREMENT PRIMARY KEY in MySQL (script issued_mysql.sql).'),
  H2('README.md of the repository'),
  ...codeLines(rd('README.md'), 14),
  H2('GitHub steps'),
  ...codeLines(rd('GITHUB_STEPS.md'), 14),

  H1('Appendix B – The Test-Only JDBC Driver and How the Evidence Was Produced'),
  P('Because no MySQL server was available, the JDBC programs were run against the real SQLite library through a small driver written for this purpose. **It is test equipment, not part of any answer.** It implements java.sql.Driver, Connection, Statement, PreparedStatement, ResultSet, ResultSetMetaData and DatabaseMetaData with dynamic proxies and calls the native SQLite functions (sqlite3_open, sqlite3_exec, sqlite3_prepare_v2, sqlite3_step, sqlite3_bind_*, sqlite3_column_*) through Java 21\'s foreign-function API. Semantics used by the experiments: Statement.executeUpdate(String) parses and executes the text every time (sqlite3_exec); a PreparedStatement is prepared once (sqlite3_prepare_v2) and re-bound and re-executed; setAutoCommit(false) issues BEGIN; commit and rollback issue COMMIT and ROLLBACK; auto-commit mode leaves every statement as its own transaction (a durable commit each); constraint violations are thrown as SQLIntegrityConstraintViolationException. Any JDBC method that is not implemented throws SQLFeatureNotSupportedException, so a program cannot silently depend on something the driver does not do.'),
  table(['Limitation', 'Effect on the results'], [
    ['SQLite is not MySQL', 'Error messages, dialect details (backslash escapes, AUTO_INCREMENT, LOAD DATA, upsert syntax) and the server-side engine differ. The experiments were chosen so that the concepts (parse cost, commit cost, atomicity, constraints, injection) are the same.'],
    ['No network', 'Batching and multi-row inserts save more with a remote server than measured here; this is stated where it matters (B3).'],
    ['Durable commits on this disk', 'The 0.8 ms per commit depends on the disk. The in-memory runs (Table B3.2) show the same experiments without disk waits.'],
    ['One machine, one run for the long cases', 'The 100,000-row auto-commit runs took about 80 s each and were run once; the repeated runs of the short cases show variation of about ±20%.'],
  ], [2700, 6326], { leftAll: true, size: 16 }),
  ...codeFile('tools/run_jdbc.sh'), ...codeFile('tools/sqlite-jdbc-shim/src/minisqlite/MiniSqlite.java', 'the test-only JDBC driver'),

  H1('Appendix C – Supporting Programs of Part A, Viva Preparation, Checklist'),
  P('Part A answers contain pseudocode only, as required. The following Java programs implement and test the Part A designs; their outputs are quoted in the answers.'),
  H2('C.1 Task A3 – generic DAO'),
  ...codeFile('TaskA3/student.sql'), ...codeFile('TaskA3/Dao.java'), ...codeFile('TaskA3/AbstractJdbcDao.java'), ...codeFile('TaskA3/Student.java'), ...codeFile('TaskA3/StudentDao.java'),
  ...codeFile('TaskA3/CourseDao.java'), ...codeFile('TaskA3/MarksDao.java'), ...codeFile('TaskA3/ScatteredStudentService.java', 'the alternative'), ...codeFile('TaskA3/ScatteredReportScreen.java'),
  ...codeFile('TaskA3/ScatteredRegistrationScreen.java'), ...codeFile('TaskA3/ScatteredMarksService.java'), ...codeFile('TaskA3/A3Demo.java'),
  H2('C.2 Task A4 – serialization'),
  ...codeFile('TaskA4/Resident.java'), ...codeFile('TaskA4/BadResident.java'), ...codeFile('TaskA4/Room.java'), ...codeFile('TaskA4/Hostel.java'), ...codeFile('TaskA4/SerializationDemo.java'),
  ...codeFile('TaskA4/v1/ResidentFixed.java'), ...codeFile('TaskA4/v1/ResidentImplicit.java'), ...codeFile('TaskA4/v1/Writer.java'),
  ...codeFile('TaskA4/v2/ResidentFixed.java'), ...codeFile('TaskA4/v2/ResidentImplicit.java'), ...codeFile('TaskA4/v2/Reader.java'), ...codeFile('TaskA4/versioning.sh'), ...codeFile('TaskA4/FormatBenchmark.java'),
  H2('C.3 Task A5 – transfer'),
  ...codeFile('TaskA5/wallet.sql'), ...codeFile('TaskA5/TransferDemo.java'),
  ...fig('diagrams/er_a5_b2.png', 460, 230, 'Figure C.1 – ER diagrams of the tables used in A5 (account) and B2 (users)'),

  H2('C.4 Viva preparation – short answers'),
  table(['Question', 'Answer'], [
    ['Why is reading a file with read() one byte at a time slow?', 'Every call is a system call. 50 MB = 52,428,784 calls (measured, 16 s). A buffer (8 KB or 64 KB) divides the number of system calls by the buffer size: 6,401 and 801 calls.'],
    ['When does BufferedInputStream not help?', 'When the program already reads blocks at least as large as the buffer, when the data is already in memory, and for random access.'],
    ['What does a transaction guarantee?', 'Atomicity: all statements take effect together (commit) or none (rollback). Measured: a failure at row 500 leaves 0 rows with a transaction and 500 rows without.'],
    ['Why is auto-commit slow for bulk loads?', 'Each statement is a transaction with a durable commit (about 0.8 ms on this disk); 100,000 rows = 80 s; one transaction = 0.3 s.'],
    ['Why is PreparedStatement safe?', 'The SQL structure is parsed before the data exists and the data are sent as values, so input can never become SQL. Verified with three attacks.'],
    ['Why is escaping quotes not enough?', 'It fails in numeric contexts (no quotes), depends on the dialect (MySQL backslash) and must be remembered for every value; regex validation blocks honest input such as it\'s-me.'],
    ['What is serialVersionUID for?', 'Compatibility check between the class and the stream. Declared explicitly, adding a field is compatible (phone = null); computed automatically, it changes and the old file gives InvalidClassException.'],
    ['What is transient?', 'A field that serialization skips (password, Connection); after reading it is null/0 and must be recreated.'],
    ['What is a DAO and why use it?', 'A class that owns all database access of one entity. The repeated JDBC steps are written once in a template class; a column change touches one class instead of four.'],
    ['Which design for 12 campuses?', 'A shared relational database with indexes: 0.025 ms per lookup against 781 ms in the text file, unique numbers from AUTO_INCREMENT, transactions, no heap proportional to the data.'],
  ], [3300, 5726], { leftAll: true, size: 16 }),
  H2('C.5 Final checklist before submission'),
  B('Replace every <placeholder> (name, register number, college, GitHub link, date).'),
  B('Run every program on your own computer; run the JDBC programs with MySQL (Appendix A) and add a screenshot of the MySQL tables; retake the screenshots from your own terminal.'),
  B('Check that the pseudocode keywords match the official SIMATS pseudocode standard.'),
  B('Create the GitHub repository CSA09-<RegisterNumber>-Assignment5 and commit task by task (at least one commit per task).'),
);

// ---------------------------------------------------------------- document
const doc = new Document({
  creator: 'Student', title: 'CSA09 Assignment 5 Report',
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
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'CSA09 – Programming in Java – Assignment 5 (CO5)   |   Page ', size: 18 }), new TextRun({ children: [PageNumber.CURRENT], size: 18 })] })] }) },
      children: body },
  ],
});
Packer.toBuffer(doc).then(b => { fs.writeFileSync(path.join(ROOT, 'report/CSA09_Assignment5_Report.docx'), b); console.log('report written', (b.length / 1048576).toFixed(1) + ' MB'); });
