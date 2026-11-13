
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
