
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
