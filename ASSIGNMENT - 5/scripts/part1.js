
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
