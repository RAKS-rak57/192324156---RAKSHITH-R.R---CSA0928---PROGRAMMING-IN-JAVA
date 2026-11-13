
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
