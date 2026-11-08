// Builds report/CSA09_Assignment2_Report.docx from the text below + the CSV files produced by the Java programs.
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun, HeadingLevel,
  AlignmentType, WidthType, ShadingType, BorderStyle, PageBreak, LevelFormat, Footer, PageNumber,
} = require('docx');

const ROOT = path.resolve(__dirname, '..');
const W = 9026; // content width in DXA (A4, 1 inch margins)

// ---------- small helpers ----------
function csv(file) {
  const lines = fs.readFileSync(path.join(ROOT, file), 'utf8').trim().split('\n');
  const head = lines[0].split(',');
  return lines.slice(1).map(l => { const c = l.split(','); const o = {}; head.forEach((h, i) => o[h] = c[i]); return o; });
}
const f1 = x => Number(x).toFixed(1), f2 = x => Number(x).toFixed(2), f3 = x => Number(x).toFixed(3);
const fmt = x => Number(x).toLocaleString('en-US');

const runs = (text, opts = {}) => {
  // **bold** support
  return text.split(/(\*\*[^*]+\*\*)/).filter(s => s.length).map(s =>
    s.startsWith('**') ? new TextRun({ text: s.slice(2, -2), bold: true, ...opts }) : new TextRun({ text: s, ...opts }));
};
const P = (text, opts = {}) => new Paragraph({ children: runs(text), spacing: { after: 110, line: 276 }, ...opts });
const H1 = t => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)], pageBreakBefore: true });
const H2 = t => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const H3 = t => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(t)] });
const B = text => new Paragraph({ numbering: { reference: 'bul', level: 0 }, children: runs(text), spacing: { after: 60, line: 276 } });
const caption = t => new Paragraph({ children: [new TextRun({ text: t, italics: true, size: 18 })], alignment: AlignmentType.CENTER, spacing: { after: 160 } });
const code = (txt) => txt.replace(/^\n/, '').split('\n').map((l, i, a) => new Paragraph({
  children: [new TextRun({ text: l.length ? l : ' ', font: 'Courier New', size: 17 })],
  shading: { type: ShadingType.CLEAR, fill: 'F2F2F2' }, spacing: { after: 0, line: 240 },
  indent: { left: 120, right: 120 }, keepLines: true,
}));
const gap = () => new Paragraph({ children: [], spacing: { after: 100 } });

const border = { style: BorderStyle.SINGLE, size: 4, color: 'AAAAAA' };
const borders = { top: border, bottom: border, left: border, right: border };
function table(headers, rows, widths, opts = {}) {
  const total = widths.reduce((a, b) => a + b, 0);
  const size = opts.size || 17;
  const mk = (txt, w, head, align) => new TableCell({
    borders, width: { size: w, type: WidthType.DXA },
    shading: head ? { type: ShadingType.CLEAR, fill: 'D9E2F3' } : undefined,
    margins: { top: 50, bottom: 50, left: 80, right: 80 },
    children: [new Paragraph({ alignment: align, children: runs(String(txt), { size, bold: head }) })],
  });
  const trs = [new TableRow({ tableHeader: true, children: headers.map((h, i) => mk(h, widths[i], true, AlignmentType.LEFT)) })];
  rows.forEach(r => trs.push(new TableRow({ cantSplit: true, children: r.map((c, i) => mk(c, widths[i], false, (i === 0 || opts.leftAll) ? AlignmentType.LEFT : AlignmentType.RIGHT)) })));
  return new Table({ width: { size: total, type: WidthType.DXA }, columnWidths: widths, rows: trs });
}
function img(file, widthPx) {
  const buf = fs.readFileSync(path.join(ROOT, file));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  const height = Math.round(widthPx * h / w);
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 80, after: 60 },
    children: [new ImageRun({ type: 'png', data: buf, transformation: { width: widthPx, height }, altText: { title: file, description: file, name: file } })] });
}
// Terminal screenshots can be very tall: scale to fit width AND a maximum height
function shot(file, maxW = 600, maxH = 560) {
  const buf = fs.readFileSync(path.join(ROOT, file));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  let sc = Math.min(maxW / w, maxH / h, 1);
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 80, after: 60 },
    children: [new ImageRun({ type: 'png', data: buf, transformation: { width: Math.round(w * sc), height: Math.round(h * sc) }, altText: { title: file, description: file, name: file } })] });
}

const body = [];
const add = (...x) => x.forEach(e => body.push(e));

// =====================================================================
// COVER PAGE
// =====================================================================
add(
  new Paragraph({ spacing: { before: 1800 }, alignment: AlignmentType.CENTER, children: [new TextRun({ text: '<YOUR COLLEGE / DEPARTMENT NAME>', bold: true, size: 28 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 }, children: [new TextRun({ text: '(write your college and department here)', italics: true, size: 20 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: [new TextRun({ text: 'CSA09 – PROGRAMMING IN JAVA', bold: true, size: 40 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: [new TextRun({ text: 'ASSIGNMENT 2 – CO2 REPORT', bold: true, size: 34 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 700 }, children: [new TextRun({ text: 'Strings, Collections Framework, hashCode–equals, Generics, Comparable / Comparator', size: 24 })] }),
  table(['Item', 'Details'], [
    ['Student name', '<YOUR NAME>'],
    ['Register number', '<REGISTER NUMBER>'],
    ['Course', 'CSA09 – Programming in Java'],
    ['Assignment / CO', 'Assignment 2 – CO2 (Sessions 15–26), Individual, 50 marks'],
    ['Java version used', 'OpenJDK 21 (java -version)'],
    ['GitHub repository', 'https://github.com/<your-username>/CSA09-<RegisterNumber>-Assignment2'],
    ['Submission date', '<DATE>'],
  ], [2600, 6426], { leftAll: true, size: 21 }),
  gap(),
  P('**Note on the repository:** the repository contains one folder per question (Q1 … Q7) with the Java source, the CSV files holding the raw measurements, the console output and the output screenshots. The README.md in the repository root explains how to compile and run every program.'),
);

// ---------- contents + method ----------
add(
  H1('Contents and Experimental Method'),
  H2('Contents'),
  table(['Section', 'Topic', 'Marks'], [
    ['Q1', 'String, StringBuilder or StringBuffer? (railway e-ticket)', '6'],
    ['Q2', 'When is ArrayList an appropriate choice? (music playlist)', '4'],
    ['Q3', 'The hashCode–equals contract (fest attendance)', '6'],
    ['Q4', 'Choosing a Map (hospital pharmacy inventory)', '6'],
    ['Q5', 'Generic ranking component (sports leaderboard)', '7'],
    ['Q6', 'Priority, order and fairness in a queue (emergency triage)', '8'],
    ['Q7', 'Day-4 design challenge: diagnose, redesign, validate', '13'],
    ['App.', 'Repository structure, how to run, list of raw data files', '–'],
  ], [1200, 6826, 1000], { leftAll: true, size: 20 }),
  gap(),
  H2('How I measured'),
  P('The whole assignment is about **Predict → Design → Implement → Measure → Analyse → Evaluate**, so every question follows the same pattern: I first wrote a prediction with the reason, then wrote the program, then measured, and then compared the prediction with the observed result.'),
  B('**Timing:** System.nanoTime() around the operation being studied. Every experiment is repeated (normally 5 times) and the **average** is reported. Before the measured runs I run a **warm-up** so that the JIT compiler has already compiled the code; without a warm-up the first numbers are much slower and misleading.'),
  B('**Memory:** I used the number of bytes allocated by the current thread (com.sun.management.ThreadMXBean.getThreadAllocatedBytes). This is more reliable than “used memory before and after” because the garbage collector can run in between. For Q1 I also print the used memory before and after, as the question asks.'),
  B('**Operation counts:** where possible I also counted operations (for example how many patients were examined in Q7). Operation counts do not change from run to run, so they are the most reliable evidence.'),
  B('**Raw data:** every program writes its numbers to a .csv file in its own folder. The tables and graphs in this report are generated from those files (scripts/make_graphs.py).'),
  B('**Honest limitation:** timings below about 1–2 ms (for example the redesign in Q7 for small n) are affected by garbage collection and JIT noise, and they change slightly every time the program is run. I therefore base my conclusions on trends over several sizes and on operation counts, not on a single value.'),
  gap(),
  P('Test machine: the programs were executed with OpenJDK 21 on Linux. (Write your own laptop details here: processor, RAM, operating system – your numbers will differ from mine, but the trends must be the same.)'),
);

// =====================================================================
// Q1
// =====================================================================
const q1w = csv('Q1/q1_workloads.csv'), q1s = csv('Q1/q1_scaling.csv'), q1t = csv('Q1/q1_threads.csv');
const wl = n => q1w.filter(r => r.workload.startsWith(n));
const g = (w, a) => wl(w).find(r => r.approach === a);
const ratio = (a, b) => (Number(a) / Number(b));
const thrBad = q1t.filter(r => r.class === 'StringBuilder' && r.expected_length !== r.actual_length).length;
const thrBadExc = q1t.filter(r => r.class === 'StringBuilder' && Number(r.exceptions) > 0).length;
const thrGoodBad = q1t.filter(r => r.class === 'StringBuffer' && r.expected_length !== r.actual_length).length;
const thrTrials = q1t.filter(r => r.class === 'StringBuilder').length;
const lastLost = q1t.filter(r => r.class === 'StringBuilder' && r.expected_length !== r.actual_length).map(r => Number(r.expected_length) - Number(r.actual_length));

add(
  H1('Q1 – Building Text Under Load: String, StringBuilder or StringBuffer?'),
  P('**Problem context:** a railway system builds an e-ticket by joining many fields (PNR, names, coach, berth, fare, journey details) into one text block. The current code uses the + operator inside a loop. I investigated whether the choice of String, StringBuilder or StringBuffer really matters.'),
  H2('1.1 Prediction before implementation'),
  P('**Reasoning.** A String is immutable. When I write ticket = ticket + field + "|" in a loop, Java cannot change the old String, so it creates a **new String object** and copies all the characters built so far plus the new field. If a ticket has n fields of average length L, the copying for field number i costs about i·L characters, so the total copying is L·(1 + 2 + … + n) = L·n(n+1)/2, which is **O(n²)**. A StringBuilder keeps one internal character array that is expanded by doubling, so each append copies only the new field (amortised O(1)) and the whole ticket costs **O(n)**. StringBuffer does the same work but every method is **synchronized**, so I expect it to be a little slower than StringBuilder when only one thread uses it.'),
  table(['Workload', 'My prediction'], [
    ['W1 – 1,000 short tickets, 10 fields each', 'Small work. The three approaches should be almost equal (difference negligible); String + may be a little slower.'],
    ['W2 – 10,000 tickets, 50 fields each', 'String + clearly slower (a few times) and it creates about 50 times more objects; StringBuilder faster; StringBuffer about 5–25% slower than StringBuilder.'],
    ['W3 – 20 tickets, 5,000 fields each (group booking / long log)', 'The quadratic effect dominates: String + should be hundreds of times slower and allocate gigabytes; StringBuilder and StringBuffer almost equal.'],
  ], [3300, 5726], { leftAll: true }),
  gap(),
  H2('1.2 Design of the workloads'),
  P('I designed three deliberately different workloads: W1 (short tickets, many repetitions so that the time can be measured), W2 (the “normal” case suggested in the question: 10,000 tickets with 50 fields) and W3 (my own workload: very long tickets, such as a group booking or a combined booking log, 5,000 fields each). In addition, a **scaling experiment** builds one ticket with n = 500, 1000, … 32,000 fields so that the growth rate can be seen directly.'),
  H2('1.3 Implementation'),
  P('The program TicketBuilder.java contains one method for each approach. The two main ones are shown below; String.join() and StringJoiner are included for the alternative-concept part.'),
  ...code(`
static String buildPlus(String[] fields) {
    String ticket = "";
    for (int i = 0; i < fields.length; i++) {
        ticket = ticket + fields[i] + "|";      // new String object every time
    }
    return ticket;
}

static String buildStringBuilder(String[] fields) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
        sb.append(fields[i]).append("|");
    }
    return sb.toString();
}`),
  gap(),
  H2('1.4 Measured results'),
  P('Average of 5 runs after warm-up. “Alloc” is the total memory allocated while building all tickets once; “Used before / after” is the heap in use before and after one run (affected by the garbage collector, so the allocation column is the better memory measure). “Est. Strings” is my estimate of the String objects created (tickets × fields for the + operator; about 2 per ticket for the others).'),
);
const t1rows = [];
q1w.forEach(r => t1rows.push([r.workload.split(' ')[0] + ' ' + r.tickets + '×' + r.fields, r.approach, f3(r.avg_time_ms), f1(r.allocated_MB), f1(r.used_before_MB) + ' → ' + f1(r.used_after_MB), fmt(r.est_objects)]));
add(
  table(['Workload', 'Approach', 'Time (ms)', 'Alloc (MB)', 'Used MB before → after', 'Est. Strings'], t1rows, [1700, 1700, 1200, 1250, 1776, 1400], { size: 16 }),
  caption('Table 1.1 – Q1 workloads (from Q1/q1_workloads.csv)'),
  img('graphs/q1_workloads.png', 430),
  caption('Figure 1.1 – Time for the three workloads (logarithmic scale)'),
);
const sc = q1s;
add(
  P('Scaling experiment: time (ms) to build **one** ticket with n fields.'),
  table(['n (fields)', 'String +', 'StringBuilder', 'StringBuffer', 'String.join', 'StringJoiner'],
    sc.map(r => [fmt(r.fields), f3(r['String +']), f3(r.StringBuilder), f3(r.StringBuffer), f3(r['String.join']), f3(r.StringJoiner)]), [1500, 1500, 1600, 1600, 1400, 1426]),
  caption('Table 1.2 – Scaling with the number of fields (from Q1/q1_scaling.csv)'),
  img('graphs/q1_scaling.png', 420),
  caption('Figure 1.2 – Log–log plot: String + rises with slope about 2, the others with slope about 1'),
  P(`**Growth rate.** When n doubled from 8,000 to 16,000, the String + time grew by a factor of ${f1(ratio(sc[5]['String +'], sc[4]['String +']))}, and from 16,000 to 32,000 by ${f1(ratio(sc[6]['String +'], sc[5]['String +']))}. A factor of about 4 for doubling n is the signature of O(n²). For StringBuilder the factor was ${f1(ratio(sc[5].StringBuilder, sc[4].StringBuilder))} and ${f1(ratio(sc[6].StringBuilder, sc[5].StringBuilder))}, close to 2 (O(n)); the small timings are noisy, which explains why it is not exactly 2.`),
  H2('1.5 Prediction compared with observation'),
  B(`**W1 (short tickets):** String + took ${f3(g('W1', 'String +').avg_time_ms)} ms against ${f3(g('W1', 'StringBuilder').avg_time_ms)} ms for StringBuilder, so the difference is negligible, as predicted. In an earlier run of the same program String + was about 4–5 times slower in W1 because the JIT had not yet optimised it, and one StringJoiner value jumped to 2 ms because of a garbage-collection pause. This showed me that very short workloads are noisy; the absolute difference is only about one millisecond per 1,000 tickets.`),
  B(`**W2:** String + took ${f1(g('W2', 'String +').avg_time_ms)} ms against ${f1(g('W2', 'StringBuilder').avg_time_ms)} ms for StringBuilder (${f1(ratio(g('W2', 'String +').avg_time_ms, g('W2', 'StringBuilder').avg_time_ms))} times slower) and allocated ${f1(g('W2', 'String +').allocated_MB)} MB against ${f1(g('W2', 'StringBuilder').allocated_MB)} MB (${f1(ratio(g('W2', 'String +').allocated_MB, g('W2', 'StringBuilder').allocated_MB))} times more). The prediction “a few times slower” was correct.`),
  B(`**W3:** String + took ${f1(g('W3', 'String +').avg_time_ms)} ms against ${f2(g('W3', 'StringBuilder').avg_time_ms)} ms (${Math.round(ratio(g('W3', 'String +').avg_time_ms, g('W3', 'StringBuilder').avg_time_ms))} times slower) and allocated about ${fmt(Math.round(g('W3', 'String +').allocated_MB))} MB, which is roughly ${Math.round(ratio(g('W3', 'String +').allocated_MB, g('W3', 'StringBuilder').allocated_MB))} times more than StringBuilder. My prediction of “hundreds of times” was correct; the allocation is the reason the garbage collector is also kept busy.`),
  B(`**StringBuffer vs StringBuilder:** StringBuffer was slower in W2 (${f1(g('W2', 'StringBuffer').avg_time_ms)} ms vs ${f1(g('W2', 'StringBuilder').avg_time_ms)} ms) and W3 (${f2(g('W3', 'StringBuffer').avg_time_ms)} ms vs ${f2(g('W3', 'StringBuilder').avg_time_ms)} ms). The cost of synchronisation is real but small when there is no contention (about 10–25%). It allocates the same memory.`),
  B('**What surprised me:** String.join() was the fastest of all. It first calculates the total length and then builds the result in one exactly-sized array, so there is no growth copying. Also, memory “used after” looked larger for StringBuilder than for String +, but that only shows how much garbage had not been collected yet; the allocation column is the true comparison.'),
  H2('1.6 Design decision: multithreaded booking server'),
  P('**Per-ticket text → StringBuilder.** Each thread builds its own ticket in a local variable, so no other thread can see the object. Synchronisation is then wasted work, and StringBuilder is the fastest of the mutable classes.'),
  P(`**Shared booking log → StringBuffer (or, better, one lock around whole entries).** I tested the shared log with 4 threads each appending 50,000 lines to the same object, repeated ${thrTrials} times. The StringBuilder lost characters in **${thrBad} of ${thrTrials} trials** (for example ${lastLost.join(', ')} characters missing) and threw an exception in ${thrBadExc} of them, because two threads updated its internal count and array at the same time. The StringBuffer gave the correct length in **${thrTrials - thrGoodBad} of ${thrTrials} trials**. The race is not seen in every run, which is exactly why it is dangerous: the code “works” in testing and fails in production.`),
  P('A limitation I must mention: StringBuffer makes each single append() call safe, but a log entry made of several appends can still be mixed with another thread’s entry. To keep a whole line together I would build the line in a local StringBuilder first and then append it with a single append call, or use a synchronized block / a ConcurrentLinkedQueue for log lines.'),
  H2('1.7 Alternative concept and complexity analysis'),
  P('If String.join(), StringJoiner or String.format() were used instead of StringBuilder, the solution would change as follows:'),
  ...code(`
String ticket1 = String.join("|", fields);            // one call, fields already in a String[] / List

StringJoiner sj = new StringJoiner("|", "[", "]");    // delimiter, prefix, suffix
for (String f : fields) sj.add(f);
String ticket2 = sj.toString();

String ticket3 = String.format("%s|%s|%s|%s", pnr, name, coach, berth);   // fixed number of fields only`),
  gap(),
  table(['Approach', 'Time for n fields', 'Extra space (garbage) created', 'Remark'], [
    ['String + in loop', 'O(n²) characters copied (L·n(n+1)/2)', 'O(n²) characters allocated in total, n String objects; only O(n) alive at one time', 'Slow and creates much garbage for large n'],
    ['StringBuilder', 'O(n) amortised', 'O(n): one builder plus one result; growth copies add at most about 2× the final length', 'Best general choice'],
    ['StringBuffer', 'O(n) (+ lock cost per call)', 'Same as StringBuilder', 'Use only when shared by threads'],
    ['String.join()', 'O(n)', 'O(n): one result array, almost no garbage', 'Fastest in my tests; needs all fields ready'],
    ['StringJoiner', 'O(n)', 'O(n): keeps the pieces then joins them', 'Convenient for delimiter / prefix / suffix'],
    ['String.format()', 'O(n) but a large constant (parses the format string every call)', 'O(n) plus a Formatter object', 'Good for readability with few fields; slowest per call'],
  ], [1700, 2300, 3100, 1926], { leftAll: true, size: 16 }),
  caption('Table 1.3 – Complexity comparison'),
  P('**Derivation.** With repeated +, step i copies the i fields already built, so the characters copied are L·(1 + 2 + … + n) = L·n(n+1)/2 = O(n²). With StringBuilder, each append copies only the new field, and the occasional array growth copies the old contents; because the capacity doubles, those copies sum to 1 + 2 + 4 + … ≤ 2·(final length), so the total is O(n). (From Java 9 the compiler turns a single expression such as a + b + c into one efficient concatenation, but a + inside a loop still creates a new String for every iteration, which is what the measurements show.)'),
  H2('1.8 Conclusion for Q1'),
  P('The choice matters when the text is long or built many times, and it hardly matters for a few short strings. For the e-ticket generator I recommend StringBuilder (or String.join when the fields are already in an array), StringBuffer only for an object shared between threads, and never + inside a loop. The evidence: 273-times slower and 570-times more allocation for + in W3, correct log length in all trials only for StringBuffer.'),
  P('Screenshot of the program output:'),
  shot('Q1/screenshot_TicketBuilder.png', 560, 520),
  caption('Figure 1.3 – Console output of TicketBuilder.java'),
);

// =====================================================================
// Q2
// =====================================================================
const q2 = csv('Q2/q2_list_results.csv'), q2u = csv('Q2/q2_upnext_results.csv'), q2a = csv('Q2/q2_array_results.csv');
const L = (n, op, st) => { const r = q2.find(x => Number(x.size) === n && x.operation === op && x.structure === st); return r ? Number(r.ns_per_op) / 1000 : null; };
const opsList = ['add at end', 'insert at front', 'insert in middle', 'remove at front', 'remove in middle', 'get k-th (random)', 'iterate whole list'];
const fm = v => v === null ? 'n/a' : (v < 0.01 ? v.toFixed(4) : v < 1 ? v.toFixed(3) : v < 100 ? v.toFixed(2) : v.toFixed(0));
add(
  H1('Q2 – When Is ArrayList an Appropriate Engineering Choice?'),
  P('**Problem context:** a music playlist editor adds songs at the end, inserts at chosen positions, removes songs and jumps to the k-th song. The app stores each playlist in an ArrayList<Song>.'),
  H2('2.1 Prediction'),
  P('ArrayList stores the references in an array. Reading position k is one array access (O(1)). Inserting or removing at position p must move all elements after p (O(n − p)), so front is the worst place. Adding at the end is O(1) amortised. LinkedList is the opposite: a node can be inserted at the front in O(1), but reaching position k means following k links (O(n)). So I predicted: ArrayList wins get(k) by a huge margin; LinkedList wins insert/remove at the front; **in the middle both are O(n)**, but I expected the LinkedList to still lose because following pointers scattered in memory is slower than the array shift, which is done by a fast System.arraycopy.'),
  H2('2.2 Experiment design'),
  B('Playlist sizes N = 10,000, 50,000 and 200,000 songs (three sizes, as required).'),
  B('For each operation, 1,000 operations are timed (the whole-list iteration is timed once and divided by N). The experiment is repeated 5 times and averaged, after a warm-up.'),
  B('Operations: add at end, insert at front, insert in middle, remove at front, remove in middle, get k-th (1,000 random positions generated with a fixed seed so all lists see the same positions), iterate the whole list with for-each.'),
  B('ArrayDeque is included where it has the operation (it has no index-based operations, so those cells are n/a).'),
  H2('2.3 Results'),
  P('Time in **microseconds per operation** (per element for iteration), average of 5 runs.'),
);
const q2rows = opsList.map(o => [o, fm(L(10000, o, 'ArrayList')), fm(L(10000, o, 'LinkedList')), fm(L(50000, o, 'ArrayList')), fm(L(50000, o, 'LinkedList')), fm(L(200000, o, 'ArrayList')), fm(L(200000, o, 'LinkedList')), fm(L(200000, o, 'ArrayDeque'))]);
add(
  table(['Operation', 'AL 10k', 'LL 10k', 'AL 50k', 'LL 50k', 'AL 200k', 'LL 200k', 'AD 200k'], q2rows, [1900, 960, 960, 960, 960, 1100, 1100, 1086], { size: 16 }),
  caption('Table 2.1 – AL = ArrayList, LL = LinkedList, AD = ArrayDeque (from Q2/q2_list_results.csv)'),
  img('graphs/q2_operations.png', 560),
  caption('Figure 2.1 – The three operations where the structures differ most'),
  H3('Observations'),
  B(`**get(k):** at N = 200,000 ArrayList needs ${fm(L(200000, 'get k-th (random)', 'ArrayList'))} µs and LinkedList ${fm(L(200000, 'get k-th (random)', 'LinkedList'))} µs, a difference of more than 1,000 times, and LinkedList grows with N exactly as O(n) predicts.`),
  B(`**Insert at front:** ArrayList grows from ${fm(L(10000, 'insert at front', 'ArrayList'))} µs (N = 10k) to ${fm(L(200000, 'insert at front', 'ArrayList'))} µs (N = 200k), i.e. linearly, while LinkedList stays at about ${fm(L(200000, 'insert at front', 'LinkedList'))} µs. ArrayDeque is also constant (${fm(L(200000, 'insert at front', 'ArrayDeque'))} µs).`),
  B(`**Insert / remove in the middle:** my prediction was right: both are O(n) but LinkedList is far worse (${fm(L(200000, 'insert in middle', 'LinkedList'))} µs against ${fm(L(200000, 'insert in middle', 'ArrayList'))} µs at 200k). The LinkedList has to walk to the middle first. So “LinkedList is good for insertion” is only true if you already hold an iterator at the position.`),
  B('**Add at end and iteration:** about the same for both (O(1) and O(n)); the LinkedList iteration is slightly slower per element because its nodes are scattered in memory (poor cache use). Small values such as 0.003 µs are close to the timer resolution.'),
  H2('2.4 Operating boundary for ArrayList'),
  P('**ArrayList is a reasonable choice** when the playlist is mostly appended to, read by index and iterated, and front or middle changes are occasional. A user playlist usually has hundreds or a few thousand songs; at N = 10,000 even a front insert costs only about 0.5 µs, which nobody can notice.'),
  P('**I would stop recommending it** when front or middle insertions/removals become a regular, high-volume part of the workload on large lists. The cost per operation is proportional to N (21 µs at 200,000 songs). A practical boundary I can defend from the data: if the system performs more than about 10,000 front/middle changes per second on lists of 100,000+ elements, the shifting alone would consume about 20% of one CPU core (10,000 × 21 µs = 0.21 s per second). Below that, ArrayList stays simpler and faster than LinkedList for everything else.'),
  H2('2.5 Changed condition: the “Up Next” queue'),
  P('The Up Next queue adds songs at both ends and removes from the front. For an ArrayList both “add at front” and “remove from front” are O(n). I measured 100,000 rounds of addLast + addFirst + removeFirst on queues already holding N songs:'),
  table(['Songs in queue', 'ArrayList (ms)', 'LinkedList (ms)', 'ArrayDeque (ms)'], q2u.filter(r => r.structure === 'ArrayList').map(r => {
    const n = r.size; const f = s => f1(q2u.find(x => x.size === n && x.structure === s).total_ms_for_100000_mixed_ops);
    return [fmt(n), f('ArrayList'), f('LinkedList'), f('ArrayDeque')];
  }), [2600, 2142, 2142, 2142]),
  caption('Table 2.2 – Up Next queue, 100,000 rounds (from Q2/q2_upnext_results.csv)'),
  img('graphs/q2_upnext.png', 380),
  caption('Figure 2.2 – Up Next queue'),
  P('ArrayList is about 100–120 times slower than ArrayDeque for this pattern (908 vs 8 ms, 1,095 vs 11 ms, 1,769 vs 15 ms). **My re-evaluation:** the original recommendation (ArrayList for the main playlist) stays, but the Up Next queue should be an **ArrayDeque**. LinkedList is also fast (O(1) at both ends), but each element needs a separate node object (about 24 bytes plus the reference) and scattered memory, and it was about 2–3.5 times slower than ArrayDeque in my runs. The price of ArrayDeque is that it has no get(k) and no insert in the middle; for an Up Next queue that is acceptable because users only look at the front and reorder rarely.'),
  H2('2.6 Alternative concept and complexity analysis'),
  table(['Operation', 'ArrayList', 'LinkedList', 'ArrayDeque', 'Plain array, manual shifting'], [
    ['Add at end', 'O(1) amortised', 'O(1)', 'O(1) amortised', 'O(1) if space; O(n) to copy into a bigger array'],
    ['Insert at front', 'O(n)', 'O(1)', 'O(1) amortised', 'O(n)'],
    ['Insert in middle', 'O(n) (fast arraycopy)', 'O(n) (walk to position)', 'not supported', 'O(n) (element-by-element shift)'],
    ['Get k-th', 'O(1)', 'O(n)', 'not supported', 'O(1)'],
    ['Iterate all', 'O(n)', 'O(n)', 'O(n)', 'O(n)'],
    ['Space per element', '1 array slot (4–8 bytes) + spare capacity up to about 50%', 'node object (about 24 bytes) + its element reference', '1 array slot + spare capacity (up to 100%)', '1 array slot, fixed capacity chosen in advance'],
  ], [1500, 1900, 1900, 1700, 2026], { leftAll: true, size: 16 }),
  caption('Table 2.3 – Complexity of the operations'),
  P('**Plain array with manual shifting.** The solution would keep a Song[] and an int size, and shift elements with a loop for each insert:'),
  ...code(`
for (int i = size; i > position; i--) arr[i] = arr[i - 1];   // shift right
arr[position] = newSong;
size++;`),
  gap(),
  P('The complexity is the same as ArrayList (O(n) insert, O(1) get), but I also have to write the resizing code myself. I measured 1,000 inserts:'),
  table(['N', 'Operation', 'Plain array (ms)', 'ArrayList (ms)'], q2a.map(r => [fmt(r.size), r.operation, f3(r.plain_array_ms), f3(r.ArrayList_ms)]), [1500, 2800, 2363, 2363]),
  caption('Table 2.4 – Plain array vs ArrayList (from Q2/q2_array_results.csv)'),
  P('In five of the six rows the manual loop is 5 to 14 times slower than ArrayList (one row at N = 10,000 even about 50 times) although both are O(n), because ArrayList uses System.arraycopy, a native block move, while my loop moves one reference per iteration. This is a good reminder that Big-O hides the constant factor.'),
  shot('Q2/screenshot_PlaylistExperiments.png', 560, 520),
  caption('Figure 2.3 – Console output of PlaylistExperiments.java'),
);

// =====================================================================
// Q3
// =====================================================================
const q3 = csv('Q3/q3_dedup_results.csv');
add(
  H1('Q3 – Engineering the hashCode–equals Contract'),
  P('**Problem context:** student ID cards are scanned at several gates and stored in a HashSet<Student>. The junior developer’s Student class overrides equals() (compares regNo) but does **not** override hashCode().'),
  H2('3.1 Tests, predictions and observed results'),
  P('For each test I wrote the prediction first (it is printed by the program before the result, in HashContractExperiments.java).'),
  table(['Test', 'Scans', 'Prediction (size) and reason', 'Observed'], [
    ['1 – appears to work', '3 different students, and the same card object scanned twice', 'Size 3. Different regNos are different students, and the repeated scan is the same object, so even the default identity hashCode is identical and equals() is true.', 'Bad class: 3, Good class: 3 – both correct'],
    ['2 – exposes duplicates', 'Arun (new object), Divya, Arun again (another new object)', 'Bad class: 3 (a duplicate!). The two Arun objects are equal by equals() but have different identity hash codes, so they go to different buckets and equals() is never asked. Good class: 2.', 'Bad: 3, Good: 2. Bad set contents: [192311002 Divya, 192311001 Arun, 192311001 Arun]'],
    ['2b – bigger', '100 students × 10 scans = 1,000 scans', 'Bad: about 1,000 (nothing removed), Good: 100.', 'Bad: 1000, Good: 100'],
    ['3a – mutate key', 'Add Meena, then change her regNo after insertion (GoodStudent, regNo not final)', 'contains() fails for the old and the new regNo, and adding the new regNo gives size 2: the object sits in the bucket of its old hash code.', 'contains(m) = false, contains(old) = false, contains(new) = false; after adding another object with the new regNo: size 2'],
    ['3b – TreeSet', 'Two different students both named Ravi, TreeSet ordered by name only', 'Size 1: TreeSet uses compare(), not equals(), and compare returns 0 for equal names.', 'r1.equals(r2) = false but TreeSet size = 1'],
  ], [1300, 2000, 3700, 2026], { leftAll: true, size: 16 }),
  caption('Table 3.1 – Test design, prediction and observation'),
  P('For Test 2 I also printed the hash codes: the two equal students had hashCode 1229416514 (bucket 5) and 2016447921 (bucket 1) in the default HashMap table of size 16, so they were stored in different buckets. For the corrected class both objects give the same hash code and the same bucket. (The exact numbers change from run to run because the default hashCode is based on object identity; the fact that they differ is what matters.)'),
  H2('3.2 Inquiry'),
  H3('Why the duplicates appear'),
  P('The contract says: **if a.equals(b) is true then a.hashCode() must equal b.hashCode()**. The junior’s class breaks it, because Object.hashCode() returns a value tied to the object’s identity. Two Student objects with the same regNo are equal but get different hash codes.'),
  H3('How HashSet uses hashCode() and equals()'),
  P('HashSet is built on a HashMap. add(x) first computes hash = x.hashCode() (spread with h ^ (h >>> 16)) and the bucket index (table.length − 1) & hash. It then looks only inside that bucket: for every stored element it first compares the stored hash with the new hash and only if they are equal calls equals(). So equals() is never reached for objects in different buckets. If the bucket is empty or no element matches, the new element is added. That is why a correct hashCode is needed for correctness and also for speed (a good spread keeps buckets short, giving O(1) average).'),
  H3('What happens when a field used in hashCode() is changed after insertion'),
  P('The element stays in the bucket calculated from the old hash code. After the change, contains() and remove() look in the bucket of the new hash code and do not find it (Test 3a). The object is “lost” inside the set (a memory leak), and the same student can be added again, which gives a duplicate. The cure is to use **immutable fields** in equals/hashCode (regNo declared private final, as in the Complete-the-Code class) or never to change such fields while the object is in a hash-based collection.'),
  H3('Correcting the class and verifying the correction'),
  P('hashCode() must use the same field(s) as equals(): only regNo (not name, because two scans may spell the name differently). I verified it in three ways: (1) the Attendance program now prints 2 instead of 3; (2) Test 2 and the 1,000-scan run give 2 and 100; (3) the program checks the contract rules – reflexive, symmetric, transitive, null returns false, and equal objects have equal hash codes – and all passed.'),
  H2('3.3 Complete the Code'),
  P('First run **without** TODO 1 (AttendanceBefore.java):'),
  shot('Q3/screenshot_AttendanceBefore.png', 420, 160),
  P('Then TODO 1 completed:'),
  ...code(`
// TODO 1 (completed): hashCode uses ONLY regNo, the same field used by equals()
@Override
public int hashCode() {
    return Objects.hash(regNo);
}`),
  gap(),
  shot('Q3/screenshot_Attendance.png', 420, 160),
  table(['Version', 'Output', 'Reason'], [
    ['Before TODO 1', 'Unique students: 3', 'Arun’s two objects have different identity hash codes → different buckets → both stored.'],
    ['After TODO 1', 'Unique students: 2', 'Both Arun objects have the same hash code (from regNo) → same bucket → equals() returns true → second add is rejected. This matches the expected output.'],
  ], [1800, 2200, 5026], { leftAll: true }),
  gap(),
  H2('3.4 Alternative concepts and complexity'),
  P('**TreeSet<Student> with a Comparator on regNo:** no hashCode is needed, because the TreeSet (a red-black tree) decides duplicates with compare() == 0: `new TreeSet<>(Comparator.comparing(s -> s.regNo))`. Every scan costs O(log n) comparisons. **HashMap<String, Student> keyed by regNo:** `map.putIfAbsent(regNo, student)`; the key is a String whose hashCode/equals are already correct, so the junior’s bug cannot happen, and I can store extra information (e.g. first gate). Cost O(1) average per scan.'),
  table(['Scans', 'Unique', 'HashSet (ms)', 'TreeSet (ms)', 'HashMap (ms)'], q3.map(r => [fmt(r.scans), fmt(r.unique), f2(r.HashSet_ms), f2(r.TreeSet_ms), f2(r.HashMap_ms)]), [1800, 1800, 1800, 1813, 1813]),
  caption('Table 3.2 – Time to process n scans, half of them repeats (from Q3/q3_dedup_results.csv)'),
  img('graphs/q3_dedup.png', 380),
  caption('Figure 3.1 – De-duplication time'),
  P('HashSet and HashMap are close to each other (HashSet is implemented with a HashMap inside, so both are O(1) average; the small differences at 1,000,000 scans come from resizing and garbage collection). TreeSet was about 2–3.6 times slower than HashSet, matching O(log n) against O(1). **Space** for n unique students: HashSet and HashMap both need a table of buckets plus one entry node per element (hash, key, value, next ≈ 32 bytes); a TreeSet needs one tree node per element (key, left, right, parent, colour ≈ 40 bytes) and no table. All are O(n). The advantage of TreeSet is that the students come out sorted by regNo, which a HashSet cannot give.'),
  shot('Q3/screenshot_HashContractExperiments.png', 580, 560),
  caption('Figure 3.2 – Console output of HashContractExperiments.java'),
);

// =====================================================================
// Q4
// =====================================================================
const q4 = csv('Q4/q4_map_results.csv');
const q4s = ['HashMap', 'TreeMap', 'LinkedHashMap', 'Hashtable', 'SortedArray+binarySearch'];
const Q = (n, s) => q4.find(r => Number(r.size) === n && r.structure === s);
const q4rows = [];
[1000, 20000, 500000].forEach(n => q4s.forEach(s => { const r = Q(n, s); q4rows.push([fmt(n), s, f2(r.put_ms), f2(r.lookup_100000_ms), f1(r.ns_per_lookup), f2(r.iterate_ms), f2(r.code_order_report_ms), f2(r.added_order_report_ms)]); }));
add(
  H1('Q4 – Choosing a Map for a Pharmacy Inventory'),
  P('**Problem context:** stock is looked up by medicine code hundreds of times a day, and every evening two reports are printed: all medicines in **code order**, and all medicines in the **order they were added**.'),
  H2('4.1 Design reasoning before implementation'),
  table(['', 'HashMap', 'TreeMap', 'LinkedHashMap'], [
    ['Inside', 'array of buckets (hash table)', 'red-black tree sorted by key', 'hash table + doubly linked list through the entries'],
    ['put / get', 'O(1) average', 'O(log n)', 'O(1) average (a little more work than HashMap to maintain the links)'],
    ['Iteration', 'O(n + capacity), no useful order', 'O(n), in key order', 'O(n), in insertion order'],
    ['Report in code order', 'needs extra work: copy keys and sort, O(n log n)', '**natural**: iterate, O(n)', 'needs extra work: copy keys and sort, O(n log n)'],
    ['Report in added order', 'needs extra work: store an “added number” in each Medicine and sort by it, O(n log n)', 'needs the same extra work, O(n log n)', '**natural**: iterate, O(n)'],
  ], [1700, 2500, 2300, 2526], { leftAll: true, size: 16 }),
  caption('Table 4.1 – What I expected before implementing'),
  P('So no single map gives both reports for free: TreeMap gives code order, LinkedHashMap gives added order, and HashMap gives neither. This is the main design tension of this question.'),
  H2('4.2 Implementation and tests'),
  P('PharmacyInventory.java stores Medicine objects (code, name, stock, addedOrder) in each map. To make the two orders different, the codes (MED0000001 … ) are inserted in **random order**. It was tested with three meaningfully different sizes: **1,000** (a small clinic), **20,000** (a large hospital) and **500,000** (a stress test). For each map I measured the time to put all medicines, 100,000 random lookups, a full iteration, and both reports, and I checked every report against the correct answer (a sorted array of the codes and the original insertion order). I also included Hashtable and a sorted array (for the alternative concept).'),
  P('A small demonstration with six medicines confirms the order behaviour:'),
  ...code(`
Added order       : [M305, M102, M550, M101, M420, M210]
HashMap keys      : [M550, M102, M101, M420, M210, M305]   (no guaranteed order)
TreeMap keys      : [M101, M102, M210, M305, M420, M550]   (code order)
LinkedHashMap keys: [M305, M102, M550, M101, M420, M210]   (added order)`),
  gap(),
  H2('4.3 Measured results (ms, average of 5 runs; 100,000 lookups)'),
  table(['N', 'Structure', 'put all', '100k lookups', 'ns / lookup', 'iterate', 'code-order report', 'added-order report'], q4rows, [800, 2100, 800, 1100, 1000, 800, 1200, 1226], { size: 15 }),
  caption('Table 4.2 – Q4 results (from Q4/q4_map_results.csv). In every row the output order matched the requirement (code_order_correct and added_order_correct are true for all structures)'),
  img('graphs/q4_lookup.png', 400),
  caption('Figure 4.1 – Cost of one lookup'),
  img('graphs/q4_reports.png', 520),
  caption('Figure 4.2 – Time of the two evening reports'),
  H3('Observations'),
  B(`**Lookup:** at N = 500,000 HashMap and LinkedHashMap need about ${f1(Q(500000, 'HashMap').ns_per_lookup)} ns per lookup and TreeMap ${f1(Q(500000, 'TreeMap').ns_per_lookup)} ns (about 17 times more), as O(1) against O(log n) predicts (log₂ 500,000 ≈ 19 String comparisons). The cost of the hash maps also rose from about 40 ns (N = 1,000) to 100 ns (N = 500,000) although O(1) is “constant”: the table no longer fits in the CPU cache.`),
  B(`**Reports:** the “natural” map is always the cheapest for its report: TreeMap produced the code-order report in ${f2(Q(500000, 'TreeMap').code_order_report_ms)} ms against ${f2(Q(500000, 'HashMap').code_order_report_ms)} ms for HashMap (which must sort), and LinkedHashMap produced the added-order report in ${f2(Q(500000, 'LinkedHashMap').added_order_report_ms)} ms against about 200–270 ms for the other two (which must sort by the addedOrder field). This confirms the “extra work” I predicted: an O(n log n) sort.`),
  B('**Order check:** all five structures produced both reports in the correct order, because the extra work (sorting) was done; so the choice is about cost, not about being able to produce the report at all.'),
  H2('4.4 Scalability inquiry'),
  P('**Acceptable response time.** For a pharmacist standing at a counter, anything below about 100 ms is instant, so I define the acceptable limit for **one lookup as 1 ms** (a deliberately strict limit, because the same server will also be doing other work), and for the evening reports **1 second** for the largest hospital size.'),
  P(`**Decision: LinkedHashMap<String, Medicine>.** Every map meets the limit by a very large margin (even TreeMap at 500,000 medicines needs only ${f1(Q(500000, 'TreeMap').ns_per_lookup / 1000)} µs per lookup). The LinkedHashMap has the fastest lookups (${f1(Q(500000, 'LinkedHashMap').ns_per_lookup)} ns at 500,000), gives the added-order report for free, and the code-order report is a single sort of the keys each evening (about ${f1(Q(20000, 'LinkedHashMap').code_order_report_ms)} ms for 20,000 medicines, about half a second at 500,000 – acceptable once a day). TreeMap is a close second (it makes the code report free but makes lookups 17 times slower and the added report expensive); since lookups are the frequent operation, I preferred the structure that makes lookups fastest.`),
  P('**Where the remaining cost comes from.** For the hash-based maps the remaining lookup cost is computing/comparing the String key and memory access (cache misses) when the table is large. For TreeMap it is about log₂ n String comparisons plus pointer chasing. For the evening report the remaining cost is the O(n log n) sort of String keys and the creation of the temporary list.'),
  P('**Why Hashtable is not preferred in new code.** Hashtable is a legacy class from Java 1.0: every method is synchronized, so threads queue up on one lock even when they only read; it forbids null keys and values; it has no ordered variant; and it is slower than HashMap in my tests (for example 85 ns against 37 ns per lookup at N = 20,000). If thread safety is needed, ConcurrentHashMap (fine-grained locking) is the modern choice; otherwise HashMap/LinkedHashMap.'),
  H2('4.5 Alternative concept and complexity analysis'),
  P('**Sorted array + Arrays.binarySearch():** keep a Medicine[] sorted by code (sort once with Arrays.sort), and find a medicine with Arrays.binarySearch(array, key, comparator).'),
  table(['', 'HashMap', 'TreeMap', 'Sorted array + binarySearch'], [
    ['Lookup', 'O(1) average', 'O(log n)', 'O(log n) (measured: 305 / 787 / 1,999 ns at 1k / 20k / 500k)'],
    ['Insertion of one new medicine', 'O(1) average', 'O(log n)', '**O(n)**: find the place with binary search, then shift elements; building the whole array once is O(n log n)'],
    ['Code-order report', 'O(n log n) (sort keys)', 'O(n)', 'O(n) (already sorted; measured 16 ms at 500k)'],
    ['Space', 'table + one entry per element (about 32–48 bytes each)', 'one tree node per element (about 40 bytes)', 'one reference per element (4–8 bytes): the smallest'],
  ], [2000, 2100, 2000, 2926], { leftAll: true, size: 16 }),
  caption('Table 4.3 – Alternative concept'),
  P('The sorted array is attractive when the stock list is **mostly read and rarely changed** (small memory, fastest code-order report) but every new medicine costs O(n) shifting, and it cannot give the added-order report without another structure. Because pharmacies add new medicines regularly during the day, I keep my decision of LinkedHashMap.'),
  shot('Q4/screenshot_PharmacyInventory.png', 580, 560),
  caption('Figure 4.3 – Console output of PharmacyInventory.java'),
);

// =====================================================================
// Q5
// =====================================================================
const q5 = csv('Q5/q5_sort_results.csv');
add(
  H1('Q5 – Deriving a Generic Ranking Component from a Principle'),
  P('**Principle given:** a ranking is correct only if the comparison is *consistent*: if A ranks above B and B above C then A must rank above C, and equal items must compare as equal. **Ranking rule:** points (higher first), then goal difference (higher first), then team name (A–Z). The component must be reusable for players, colleges and quiz teams.'),
  H2('5.1 My design, developed before implementation'),
  P('**Turning the principle into rules for compare(a, b).** I translated the principle into four requirements that a comparator must satisfy: (1) **antisymmetry** – sign(compare(a,b)) = −sign(compare(b,a)); (2) **transitivity** – if a ranks above b and b above c then a above c; (3) **equal means equal** – compare(a,b) = 0 means a and b are interchangeable in every comparison with any c; (4) it must never overflow or depend on the order of the input.'),
  H3('Comparable or Comparator?'),
  P('I decided to use **Comparator objects**, and to keep Comparable only as an optional default. Reasons: a Team has several sensible orders (by rank, by name for a fixture list, by goal difference), and Comparable allows only **one** natural order inside the class; the Leaderboard must be reused for players and colleges, where the “rank order” is different, and I cannot add compareTo to classes I do not own; and with a Comparator I can change the ranking rule (e.g. a new tie-breaker) without touching Team. Comparable is still useful when a type has one obvious order, so I also made a bounded-type board for that case.'),
  H3('Expressing the multi-field comparison'),
  ...code(`
Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
        .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
        .thenComparing(t -> t.name);`),
  gap(),
  P('Two details matter. First, **reversed() must be applied to each key’s comparator before chaining**; if I wrote comparingInt(points).thenComparing(goalDiff).thenComparing(name).reversed(), the name would also be reversed (Z–A), which is wrong. Second, I use Integer.compare through comparingInt and never write b.points − a.points, because subtraction can overflow (shown in the tests below).'),
  H3('Generic Leaderboard<T> and wildcards'),
  B('**Leaderboard<T>** stores a List<T> and a Comparator<? super T>. The wildcard `? super T` is used because the comparator only *consumes* T values: a Comparator<Team> can rank a Leaderboard<CollegeTeam> (shown in the test program). The ranking() method sorts a **copy**, so the stored list is not changed.'),
  B('**Bounded type:** `NaturalBoard<T extends Comparable<? super T>>` works for any type that can compare itself, and gives a compile-time error for types that cannot.'),
  B('**Where `List<? extends T>` is useful:** an addAll(List<? extends T> more) method can accept a List<CollegeTeam> into a board of Team, because the list only *produces* T values. This follows the rule “producer extends, consumer super”.'),
  H2('5.2 Tests'),
  P('**Data Set A** (several ties at every level, including two completely identical teams) and **Data Set B** (designed to break faulty comparators) are in DesignTests.java. I also wrote a checker that tests the principle on **every pair and triple** of items (antisymmetry, transitivity, consistent equal items).'),
  ...code(`
Ranking with comparator chain : [Eagles(9,6), Falcons(9,6), Falcons(9,6), Bisons(9,4), Panthers(9,4),
   Hawks(6,3), Cobras(6,1), Wolves(6,1), Sharks(3,0), Lions(3,-2), Tigers(3,-2)]
Principle check on every pair/triple of Data Set A -> violations = 0`),
  gap(),
  P('Ties are broken correctly at every level: Eagles/Falcons are tied on points and goal difference and are ordered by name; the two identical Falcons are equal; Cobras/Wolves tie on points and goal difference and are ordered A–Z.'),
  table(['Comparator', 'Result on Data Set B', 'Violations found by the checker'], [
    ['Correct chain', 'Alpha (2,000,000,000 pts) first, Bravo (−2,000,000,000 pts) last, correct tie-breaks', '0'],
    ['Faulty 1: points compared by subtraction', 'Alpha is placed **below** Bravo (wrong): 2,000,000,000 − (−2,000,000,000) overflows int and becomes negative', '12 (e.g. Alpha > Charlie > Bravo, but not Alpha > Bravo – transitivity broken)'],
    ['Faulty 2: ignores the tie-breakers', 'Same teams in a different input order give a different ranking (Charlie, Delta, Echo, Zulu vs Zulu, Echo, Delta, Charlie): it is not a function of the data', 'equal items are not really equal'],
  ], [2500, 4300, 2226], { leftAll: true, size: 16 }),
  caption('Table 5.1 – Breaking faulty comparators'),
  P('I also sorted 2,000 random teams with extreme points using the subtraction comparator. The JDK did not throw an exception in this run (TimSort only sometimes detects a broken comparator with “Comparison method violates its general contract”), but the order was still wrong. So “no exception” is not a proof of correctness – this is why the pair/triple checker is useful.'),
  H2('5.3 Correctness inquiry – why my comparator satisfies the principle'),
  P('Reasoning, not only program output: the chain compares the keys in the order (points descending, goal difference descending, name ascending) and moves to the next key **only if all earlier keys are equal**. This is the **lexicographic order** on the tuple (−points, −goalDiff, name). (a) Each single key comparison (Integer.compare, String.compareTo) is a total order on its key, and reversing a total order gives a total order. (b) For two teams, find the first key where they differ; the result is decided by that key, and swapping the teams swaps the sign at the **same** key, so **antisymmetry** holds. (c) **Transitivity:** if A ranks above B (first difference at key i) and B above C (first difference at key j), take k = min(i, j). On all keys before k the three teams are identical, and at key k either A is above B (or B is above C) strictly while the other pair is not below, so A is above C at key k; therefore A ranks above C. (d) **Equal items:** compare(A,B) = 0 only when all three keys are equal, and then A and B behave identically against every C. Using Integer.compare means there is no overflow. Therefore the comparator is a consistent total preorder, which is exactly the principle.'),
  H2('5.4 Complete the Code'),
  ...code(`
List<T> ranking() {
    // TODO 1 (completed): new sorted copy, items is not modified
    List<T> copy = new ArrayList<>(items);
    copy.sort(order);
    return copy;
}
// in main:
// TODO 2 (completed): the comparator chain shown in section 5.1
Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
        .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
        .thenComparing(t -> t.name);`),
  gap(),
  shot('Q5/screenshot_LeaderboardDemo.png', 560, 150),
  P('The output matches the expected output exactly. **Why Bisons is ranked above Panthers:** both have 9 points and goal difference 4, so the first two keys are equal and the comparator reaches the third key, the name; "Bisons" comes before "Panthers" alphabetically (B before P), so Bisons ranks higher. Eagles is first because its goal difference (6) is higher than 4 with the same points.'),
  H2('5.5 Alternative concept and complexity analysis'),
  table(['Design', 'How it works', 'Sorts', 'Time', 'Extra space', 'Reuse for players / colleges'], [
    ['Comparator chain (my design)', 'one sort with the chain comparator', '1', 'O(n log n) comparisons, each up to 3 key checks', 'TimSort temporary array, up to n/2 references = O(n); a few comparator objects', 'Easy: write a new comparator, Leaderboard<T> unchanged'],
    ['Comparable<Team>', 'compareTo contains the same 3 rules; Collections.sort(list)', '1', 'O(n log n)', 'same O(n) temporary array', 'Harder: one fixed order per class; every class needs its own compareTo; no second ordering'],
    ['3 stable sorts', 'sort by name, then by goal difference, then by points (least important key first)', '3', '3 × O(n log n) = still O(n log n) but with about 3× the constant', 'O(n) temporary array each time (reused after each sort)', 'Fragile: correct only if the order of passes is right and the sort is stable'],
  ], [1500, 1900, 600, 1500, 1800, 1726], { leftAll: true, size: 15 }),
  caption('Table 5.2 – Alternative designs'),
  P('Measured on n random teams with many ties (average of 5 runs):'),
  table(['n teams', 'Comparator chain, 1 sort (ms)', 'Comparable, 1 sort (ms)', '3 stable sorts (ms)', 'Same order?'], q5.map(r => [fmt(r.n), f2(r.comparator_chain_1sort_ms), f2(r.comparable_1sort_ms), f2(r.three_stable_sorts_ms), r.same_result]), [1500, 2300, 1900, 1900, 1426]),
  caption('Table 5.3 – Sorting time (from Q5/q5_sort_results.csv); List.sort and Collections.sort were both used'),
  img('graphs/q5_sorts.png', 380),
  caption('Figure 5.1 – Ranking designs'),
  P('All three designs gave exactly the same order, so they are equally correct. Comparable was about 1.5 times faster than the comparator chain (it avoids calling several comparator objects), and three sorts took about 1.4–1.8 times as long as one chain sort – less than 3× because TimSort exploits the partly ordered data left by the earlier sort. For **reuse**, the comparator chain is the easiest, which is why I chose it: the same Leaderboard<T> works for players or quiz teams by passing a different Comparator, and Comparable is the better choice only for a class with one obvious natural order.'),
  shot('Q5/screenshot_DesignTests.png', 580, 560),
  caption('Figure 5.2 – Console output of DesignTests.java'),
);

// =====================================================================
// Q6
// =====================================================================
const q6g = csv('Q6/q6_growth_results.csv'), q6a = csv('Q6/q6_alternatives_results.csv');
add(
  H1('Q6 – Priority, Order and Fairness in a Queue'),
  P('**Problem context:** an emergency department gives each patient a severity from 1 (critical) to 5 (minor). The doctor must always see the most severe patient next; among equal severities, the patient who arrived first.'),
  H2('6.1 Design'),
  B('**Why a plain Queue is not enough.** A Queue (LinkedList or ArrayDeque) is first-in-first-out: it serves strictly by arrival. In my 8-patient example it would serve Chitra (S5, arrived third) before Devi (S1, arrived fourth), which is dangerous. A Queue has no way to “skip ahead”.'),
  B('**PriorityQueue with a Comparator.** A PriorityQueue is a binary heap that always keeps the “smallest” element (according to its comparator) at the head. With the comparator “severity number ascending” the most critical patient is always at the head.'),
  B('**Preserving arrival order among equal severities.** A heap is **not stable**: two elements that compare equal come out in an unpredictable order. I therefore give every patient an increasing arrival counter (counter++ when admitted) and compare (severity, arrival). Because the counter is unique, two patients are never “equal”, so the order is fully determined. My experiment shows the problem: with a severity-only comparator the output order was Esha before Arjun (both S3), although Arjun arrived first.'),
  B('**Next patient and display.** next() = queue.poll(), which removes the head. To display the waiting list **in treatment order** I copy the queue (new PriorityQueue<>(queue)) and poll the copy until empty (O(n log n)), or put the elements into a list and sort with the same comparator; the original queue is untouched.'),
  H2('6.2 Correctness check with 8 patients (worked out by hand)'),
  table(['Arrival #', 'Patient', 'Severity'], [
    ['0', 'Arjun', '3'], ['1', 'Bala', '2'], ['2', 'Chitra', '5'], ['3', 'Devi', '1'], ['4', 'Esha', '3'], ['5', 'Farid', '2'], ['6', 'Gita', '1'], ['7', 'Hari', '4'],
  ], [2200, 3400, 3426]),
  P('**By hand:** severity 1 first, in arrival order: Devi (3), Gita (6); then severity 2: Bala (1), Farid (5); severity 3: Arjun (0), Esha (4); severity 4: Hari; severity 5: Chitra. Expected order: **Devi, Gita, Bala, Farid, Arjun, Esha, Hari, Chitra**.'),
  ...code(`
System.out.println(priorityQueue):
[Devi(S1), Bala(S2), Gita(S1), Arjun(S3), Esha(S3), Chitra(S5), Farid(S2), Hari(S4)]
Treatment order using poll()  : Devi Gita Bala Farid Arjun Esha Hari Chitra
Hand-worked expected order    : Devi Gita Bala Farid Arjun Esha Hari Chitra
Program matches hand calculation : true`),
  gap(),
  P('The program matches the hand calculation. **Why printing the PriorityQueue directly does not show the treatment order:** PriorityQueue.toString() prints the internal array of the binary heap in level order. The heap only guarantees that every parent is not larger than its children, not that the array is sorted; only the first element (the head) is guaranteed to be the next patient. In the printed array, Gita (S1) comes after Bala (S2). Only repeated poll() gives the sorted order.'),
  H2('6.3 Complete the Code'),
  ...code(`
// TODO 1 (completed)
private PriorityQueue<Patient> queue = new PriorityQueue<>(
        Comparator.comparingInt((Patient p) -> p.severity)
                  .thenComparingLong(p -> p.arrival));`),
  gap(),
  shot('Q6/screenshot_Triage.png', 560, 150),
  P('The output equals the expected output. **How the arrival counter keeps equal severities in arrival order:** the counter is increased for each admitted patient, so Meena (counter 1) has a smaller arrival value than Kumar (counter 4). When two patients have the same severity, the comparator’s second rule thenComparingLong(arrival) compares those values and the smaller one (the earlier arrival) is placed first. The same rule gives Ravi (counter 0) before John (counter 2) for severity 3.'),
  H2('6.4 Growth investigation'),
  P('**Prediction:** add costs O(log n) in the worst case (the new element may move up the heap), but for random data the average is close to O(1) because a new element usually moves up only a level or two. Removing the head (poll) takes O(log n): the last element is moved to the root and sifted down, up to about 2·log₂ n comparisons. So when n grows by a factor of 1,000 (1,000 → 1,000,000), log₂ n only doubles (10 → 20), so I expected poll to become only about 2–3 times slower, and add to stay roughly constant.'),
  table(['n patients', 'add (ns / op)', 'poll (ns / op)', 'all adds (ms)', 'all polls (ms)'], q6g.map(r => [fmt(r.n), f1(r.add_ns_per_op), f1(r.poll_ns_per_op), f2(r.total_add_ms), f2(r.total_poll_ms)]), [1800, 1800, 1800, 1813, 1813]),
  caption('Table 6.1 – PriorityQueue cost (average of 30 runs; 5 runs for n = 1,000,000), from Q6/q6_growth_results.csv'),
  img('graphs/q6_growth.png', 380),
  caption('Figure 6.1 – Cost per operation'),
  P(`**Observed:** add stayed between about ${f1(Math.min(...q6g.map(r => Number(r.add_ns_per_op))))} and ${f1(Math.max(...q6g.map(r => Number(r.add_ns_per_op))))} ns (almost constant), and poll rose from about ${f1(q6g[0].poll_ns_per_op)} ns to ${f1(q6g[3].poll_ns_per_op)} ns, i.e. about ${f1(Number(q6g[3].poll_ns_per_op) / Number(q6g[0].poll_ns_per_op))} times for 1,000 times more patients. This agrees with the prediction (logarithmic growth, plus slower memory access when the heap is big). A linear structure would have become 1,000 times slower.`),
  H2('6.5 Starvation and a fair rule'),
  P('**Why a severity-5 patient may wait indefinitely.** The comparator compares severity first. As long as at least one patient with a smaller severity number is in the queue, the severity-5 patient can never reach the head. If critical patients keep arriving at least as fast as the doctor can treat them, the head is always a more severe patient. The arrival counter does not help, because it is used only when the severity is equal.'),
  P('**Fair rule – aging.** Every 50 time units of waiting lowers a patient’s *effective severity* by one level (never below 1): effective = max(1, severity − waited / 50). The queue is ordered by (effective severity, arrival). A severity-5 patient is then treated at the latest after 4 × 50 = 200 time units, and a severity-1 patient is never delayed by more than the effects of other patients who have also waited long.'),
  ...code(`
Plain PriorityQueue : severity-5 patient served at time NEVER (still waiting after 2000 time units)
Fair rule (aging, limit 50) : severity-5 patient served at time 200`),
  gap(),
  P('In this worst-case simulation (one new critical patient arrives and one patient is served at every time unit) the plain queue never serves the severity-5 patient, while the fair rule serves him at exactly time 200, as designed. (Note: the fair rule changes priorities over time, so a normal PriorityQueue cannot keep them; my simple implementation rebuilds the queue before every serve. Its cost is analysed in Q7.)'),
  H2('6.6 Deployment decision for 5,000 patients a day'),
  ...code(`
Plain PriorityQueue : admit + serve 5,000 patients = 8.98 ms in total (1.797 microseconds per patient)
Fair (rebuild) rule : admit + serve 5,000 patients = 286.48 ms in total (57.296 microseconds per patient)`),
  gap(),
  P('5,000 patients per day is about one patient every 17 seconds. Even in the worst case where all 5,000 are waiting together, the plain priority queue needs about 9 ms for the whole day, and my growth test handled 1,000,000 patients in well under a second. **So yes, the design can serve a city hospital with 5,000 patients a day** – the data structure is thousands of times faster than the doctors; the real bottleneck is clinical capacity. The fair version with rebuilding also works for 5,000 (0.3 s for the whole day) but its cost grows with n² (see Q7), so it is not suitable for much larger queues. Limits of my evidence: single-threaded test; a real system also needs thread safety (e.g. PriorityBlockingQueue) and persistence.'),
  H2('6.7 Alternative concept and complexity analysis'),
  table(['Design', 'Admit', 'Serve next', 'Space', 'Remark'], [
    ['PriorityQueue (my design)', 'O(log n)', 'O(log n)', 'O(n) – one array', 'General: works for any number of priority levels'],
    ['Sorted LinkedList (insert in position)', 'O(n): walk to the right position (first patient with larger severity)', 'O(1): pollFirst()', 'O(n) – one node object (about 24 bytes) per patient', 'Simple to read but slow to admit'],
    ['Array of 5 FIFO queues (one per severity)', 'O(1): addLast to queue [severity − 1]', 'O(1): check the 5 queues in order (at most 5 steps, a constant)', 'O(n) – 5 ArrayDeques', 'Fast, but only for a small fixed number of levels'],
  ], [2100, 2000, 1900, 1500, 1526], { leftAll: true, size: 16 }),
  caption('Table 6.2 – Alternatives'),
  table(['n', 'Design', 'admit all (ms)', 'serve all (ms)'], q6a.map(r => [fmt(r.n), r.design, f3(r.admit_all_ms), f3(r.serve_all_ms)]), [1500, 3300, 2113, 2113]),
  caption('Table 6.3 – Measured (from Q6/q6_alternatives_results.csv; average of 5 runs after warm-up)'),
  img('graphs/q6_alternatives.png', 380),
  caption('Figure 6.2 – Admit cost of the three designs'),
  P('The measurements match the predictions: the sorted LinkedList admit time grows roughly with n² for n admissions (0.7 ms → 64 ms → 656 ms as n grows 1,000 → 5,000 → 20,000), while the PriorityQueue and the five FIFO queues stay small. For serving, the sorted LinkedList is O(1), as predicted. The five queues are the fastest here, but they depend on having only five severity levels, whereas the PriorityQueue still works if the priority is a score with thousands of values.'),
  shot('Q6/screenshot_TriageExperiments.png', 580, 560),
  caption('Figure 6.3 – Console output of TriageExperiments.java'),
);

// =====================================================================
// Q7
// =====================================================================
const q7 = csv('Q7/q7_results.csv'), q7b = csv('Q7/q7_redesign_large.csv');
const O = n => q7.find(r => Number(r.n) === n && r.design.startsWith('Original')), R = n => q7.find(r => Number(r.n) === n && r.design.startsWith('Redesign'));
const q7rows = [];
[1000, 2500, 5000, 10000, 20000].forEach(n => {
  q7rows.push([fmt(n), 'Original (rebuild PQ)', f2(O(n).avg_time_ms), fmt(O(n).patients_or_heads_examined), fmt(O(n).priority_queues_created), f1(O(n).allocated_MB), O(n).same_order]);
  q7rows.push([fmt(n), 'Redesign (5 queues)', f2(R(n).avg_time_ms), fmt(R(n).patients_or_heads_examined), '0', f1(R(n).allocated_MB), R(n).same_order]);
});
add(
  H1('Q7 – Day-4 Design Challenge: Diagnose, Redesign and Validate'),
  P('**Solution selected:** the fair emergency-triage queue from Q6 (aging rule). From the evidence in Q6 it is the solution whose behaviour changes most with the data size: it costs 57 µs per patient for 5,000 patients (against 1.8 µs for the plain queue) and, as the experiment below shows, its total cost grows with the *square* of the queue length.'),
  H2('A. Diagnose the bottleneck (2 marks)'),
  P('**Original solution.** Because the effective severity of every patient changes as time passes, a normal PriorityQueue (whose order is fixed when the elements are inserted) cannot be used directly. My Q6 solution therefore **rebuilds the PriorityQueue before every serve**: it creates a new PriorityQueue with a comparator using the current time, adds all waiting patients into it, and polls one.'),
  ...code(`
Patient serve(long now) {
    Comparator<Patient> cmp = comparingInt(p -> effective(p, now, waitLimit)).thenComparingLong(p -> p.seq);
    PriorityQueue<Patient> fresh = new PriorityQueue<>(cmp);   // new queue + new array
    fresh.addAll(queue);                                       // re-examines EVERY waiting patient
    queue = fresh;
    return queue.poll();
}`),
  gap(),
  table(['Question', 'Diagnosis from my evidence'], [
    ['Where is most of the time / memory spent?', 'In fresh.addAll(queue): every serve re-inserts all waiting patients into a new heap. For n = 20,000 the program re-examined 200,010,000 patients in total, took 4.6 s and allocated 2.76 GB of memory (almost all of it temporary queues and arrays that become garbage).'],
    ['Which operation grows significantly with the data?', 'serve(). Its cost is proportional to the queue length, so serving n patients costs 1 + 2 + … + n = n(n+1)/2 = O(n²). The examined-patient count is exactly n(n+1)/2 (for n = 20,000: 200,010,000), and time grew about 3.2 times when n doubled from 5,000 to 10,000 and about 3.5 times from 10,000 to 20,000 (close to the factor 4 expected for n²).'],
    ['Is work unnecessarily repeated?', 'Yes. When one time unit passes, the relative order of most patients does not change, yet the whole queue is re-sorted and rebuilt from scratch before every single serve; 5,000 PriorityQueue objects were created for 5,000 patients.'],
    ['Which characteristic of the chosen class causes the limitation?', 'A PriorityQueue (a heap) assumes that an element’s priority does not change while it is inside – the same kind of problem as changing a field used in hashCode() in Q3. The heap cannot be “updated” cheaply for all elements, so my only option with that class was to rebuild it.'],
  ], [2700, 6326], { leftAll: true, size: 16 }),
  caption('Table 7.1 – Diagnosis'),
  H2('B. Design objective (2 marks)'),
  P('The redesign must: (1) serve the next patient in time that **does not depend on the queue length n** (a constant number of steps per serve), and so reduce the total examined patients for n = 20,000 from about 2 × 10⁸ to at most 5 per serve (≤ 100,000 in total); (2) create **no new queue objects per serve** (target: 0 temporary queues, memory for the whole run below 1 MB instead of 2.7 GB); (3) preserve **correctness**: the order of service must be *identical* to the original for the same input, including all ties; (4) be at least 100 times faster for n = 20,000.'),
  H2('C. Design of the revised solution (4 marks)'),
  P('**What I changed.** I replaced the single PriorityQueue by **five FIFO queues, one per severity level** (ArrayDeque). A patient is added at the tail of the queue for his severity. To serve, I do **not** look at all patients: I compare only the **head** (oldest patient) of each of the five queues using the effective severity at the current time, and remove the best one.'),
  P('**Which property I exploit.** Inside one severity queue, patients are in arrival order, and arrival times never decrease. A patient who arrived earlier has waited longer, so his effective severity is **less than or equal to** that of anyone behind him in the same queue. Therefore the best patient of each level is always its head, and the best patient overall is one of only five heads. Ties are resolved by arrival number (seq), exactly as in the original, so the order is identical.'),
  P('**Operations avoided or reduced.** Re-adding all waiting patients, creating a new heap and array, and comparing every patient with every other are avoided. Admit is O(1); serve is O(K) with K = 5 (a constant).'),
  P('**Extra memory / preprocessing.** None: five empty deques are created once. Memory use per patient is about the same as before (one array slot).'),
  P('**New trade-offs.** (1) The number of levels K is fixed (here 5): the cost per serve is O(K), so with thousands of levels a heap of heads would be needed. (2) The idea works only because the aging rule depends only on severity and arrival time; a rule that also depended on, say, the patient’s age would break the “head is the best” property. (3) It needs the clock to be non-decreasing, which is natural for arrival times.'),
  P('**Design representation (pseudocode and class sketch).**'),
  ...code(`
class FiveQueueTriage
    fields : ArrayDeque<Patient>[] q = new ArrayDeque[5]     // q[0] = severity 1 ... q[4] = severity 5
             long waitLimit

    admit(p)           :  q[p.severity - 1].addLast(p)                       // O(1)

    serve(now)         :  best = null
                          for level = 0 .. 4                                 // 5 steps, constant
                              head = q[level].peekFirst()                    // oldest of that level
                              if head != null and
                                 (best == null or better(head, best, now))   // effective severity, then seq
                                  best = head
                          return best == null ? null : q[best.severity - 1].pollFirst()

    better(a, b, now)  :  ea = max(1, a.severity - (now - a.time) / waitLimit)
                          eb = max(1, b.severity - (now - b.time) / waitLimit)
                          return ea < eb  or  (ea == eb and a.seq < b.seq)`),
  gap(),
  table(['Class', 'Fields', 'Methods'], [
    ['Patient', 'name, severity, seq (arrival number), time (arrival time)', 'toString()'],
    ['RebuildTriage (original)', 'PriorityQueue queue, waitLimit, counters', 'admit(p), serve(now)'],
    ['FiveQueueTriage (redesign)', 'ArrayDeque<Patient>[5] q, waitLimit, counter', 'admit(p), serve(now)'],
    ['TriageFair (test driver)', '–', 'drain…(), main(): equivalence tests, timing, counting'],
  ], [2700, 3900, 2426], { leftAll: true, size: 16 }),
  caption('Table 7.2 – Classes'),
  H2('D. Prediction before implementing (1 mark)'),
  P('Written before running the experiment: (1) the redesign examines at most 5 queue heads per serve, so for n patients about 5n in total (n = 20,000 → about 100,000), while the original examines n(n+1)/2 (200,010,000); the operation count should therefore improve by a factor that **grows with n** (about 2,000× at n = 20,000). (2) Time should improve by at least 100× at n = 20,000, and the redesign’s time should grow roughly linearly (not quadratically) with n. (3) Memory allocated should drop from gigabytes to well under 1 MB. (4) The order of service must be identical in all tests. The measure where I expect the clearest improvement is the **number of patients examined**, because timings of a few milliseconds are noisy.'),
  H2('E. Implement and validate (2 marks)'),
  P('**Same test conditions for both designs:** n patients all waiting (100 arrivals per time unit, random severities with a fixed seed), wait limit 500 time units, and the doctors serve 10 patients per time unit until the queue is empty; average of 5 runs (3 for n ≥ 10,000) after warm-up. I also ran **300 random mixed sequences** (admit and serve steps in random order, small wait limits so that aging happens often); 40,237 service decisions were compared and there were **0 mismatches**, so the redesign gives exactly the same order as the original.'),
  table(['n', 'Design', 'Time (ms)', 'Patients / heads examined', 'PQs created', 'Allocated (MB)', 'Same order'], q7rows, [800, 2000, 1000, 2100, 900, 1200, 1026], { size: 15 }),
  caption('Table 7.3 – Original vs redesign (from Q7/q7_results.csv)'),
  img('graphs/q7_comparison.png', 400),
  caption('Figure 7.1 – Time to serve all n patients (logarithmic axes)'),
  img('graphs/q7_operations.png', 400),
  caption('Figure 7.2 – Operation count: n²/2 against about 5n'),
  P(`The redesign alone on larger queues (the original would take minutes): n = 100,000 took ${f2(q7b[0].avg_time_ms)} ms and n = 1,000,000 took ${f2(q7b[1].avg_time_ms)} ms, i.e. about ${f1(q7b[1].ns_per_serve)} ns per serve in both cases – a constant cost per serve, so the total time grows linearly. The heads examined were ${fmt(q7b[1].heads_examined)} for 1,000,000 patients (almost exactly 5 per serve).`),
  H2('F. Defend the final design (2 marks)'),
  B(`**Was the prediction supported?** Yes. The examined count fell from ${fmt(O(20000).patients_or_heads_examined)} to ${fmt(R(20000).patients_or_heads_examined)} at n = 20,000 (${fmt(Math.round(Number(O(20000).patients_or_heads_examined) / Number(R(20000).patients_or_heads_examined)))} times fewer; at most 5 heads per serve), no priority queues were created, and the order was identical in all comparisons.`),
  B(`**Magnitude of improvement:** at n = 20,000 the time fell from ${fmt(Math.round(O(20000).avg_time_ms))} ms to ${f2(R(20000).avg_time_ms)} ms (more than 1,000 times faster; the exact ratio depends on timer noise at about 1 ms) and the allocated memory from ${f1(O(20000).allocated_MB)} MB to ${f1(R(20000).allocated_MB)} MB. The objective (≥ 100× faster, no temporary queues, identical order) was achieved. The redesign’s timings for n between 1,000 and 20,000 are all of the order of a millisecond and are not perfectly monotonic (noise from the JIT compiler and the garbage collector); the operation counts and the 100,000 / 1,000,000 runs show the trend clearly.`),
  B('**New costs and trade-offs:** the number of severity levels is fixed (K = 5) and the structure relies on the monotone-time property of the aging rule; it is a special-purpose design, less general than a PriorityQueue.'),
  B('**When the original may still be preferable:** if the system has no aging (plain priority), the Q6 PriorityQueue is simpler, has O(log n) operations and supports any number of priority values. For small queues (a few dozen patients) the rebuild cost is negligible and the simple code is easier to read. And if the priority rule was something that cannot be split into FIFO levels (for example a continuous score), the five-queue idea would not apply.'),
  P('**Final engineering conclusion:** the limitation came from the characteristics of PriorityQueue (fixed priorities) combined with a time-dependent rule, not from a lack of speed of the class itself. By understanding the problem and using the structure of the data (FIFO inside each level, monotone time) I reduced the work per serve from O(n) to O(1) without changing the result.'),
  shot('Q7/screenshot_TriageFair.png', 580, 480),
  caption('Figure 7.3 – Console output of TriageFair.java'),
);

// =====================================================================
// APPENDIX
// =====================================================================
add(
  H1('Appendix – Repository, How to Run and Raw Data'),
  H2('A. Repository structure'),
  ...code(`
CSA09-<RegisterNumber>-Assignment2/
  README.md
  Q1/  TicketBuilder.java, output.txt, q1_workloads.csv, q1_scaling.csv, q1_threads.csv, screenshot_TicketBuilder.png
  Q2/  PlaylistExperiments.java, output.txt, q2_list_results.csv, q2_upnext_results.csv, q2_array_results.csv, screenshot_...png
  Q3/  AttendanceBefore.java, Attendance.java, HashContractExperiments.java, outputs, q3_dedup_results.csv, screenshots
  Q4/  PharmacyInventory.java, output.txt, q4_map_results.csv, screenshot_PharmacyInventory.png
  Q5/  LeaderboardDemo.java, DesignTests.java, outputs, q5_sort_results.csv, screenshots
  Q6/  Triage.java, TriageExperiments.java, outputs, q6_growth_results.csv, q6_alternatives_results.csv, screenshots
  Q7/  TriageFair.java, output.txt, q7_results.csv, q7_redesign_large.csv, screenshot_TriageFair.png
  graphs/   (all figures used in this report)
  scripts/  make_graphs.py, make_screenshots.py
  report/   this report`),
  gap(),
  H2('B. How to compile and run'),
  P('Open a terminal in the question folder, for example:'),
  ...code(`
cd Q1
javac TicketBuilder.java
java TicketBuilder

cd ../Q3
javac *.java
java AttendanceBefore        (before TODO 1: prints 3)
java Attendance              (after TODO 1: prints 2)
java HashContractExperiments

cd ../Q5
javac *.java                 (LeaderboardDemo.java and DesignTests.java are compiled together)
java LeaderboardDemo
java DesignTests

cd ../Q6
javac *.java
java Triage
java TriageExperiments`),
  gap(),
  H2('C. Raw experimental data files'),
  table(['File', 'Content'], [
    ['Q1/q1_workloads.csv, q1_scaling.csv, q1_threads.csv', 'Time, allocation, estimated objects for the 3 workloads; scaling with n; 20 thread trials'],
    ['Q2/q2_list_results.csv, q2_upnext_results.csv, q2_array_results.csv', 'Time per operation for the 3 structures at 3 sizes; Up Next queue; plain array'],
    ['Q3/q3_dedup_results.csv', 'HashSet, TreeSet and HashMap de-duplication time'],
    ['Q4/q4_map_results.csv', 'put, lookup, iteration and both reports for 5 structures at 3 sizes, with order-check results'],
    ['Q5/q5_sort_results.csv', 'Comparator chain vs Comparable vs 3 stable sorts'],
    ['Q6/q6_growth_results.csv, q6_alternatives_results.csv', 'PriorityQueue growth; sorted list and five-queue alternatives'],
    ['Q7/q7_results.csv, q7_redesign_large.csv', 'Original vs redesign; redesign at 100,000 and 1,000,000 patients'],
  ], [4200, 4826], { leftAll: true, size: 16 }),
  gap(),
  P('Every table and graph in this report was produced from these files. If the programs are run again the numbers will change a little (different hardware, JIT and garbage-collection timing), but the trends and the operation counts stay the same.'),
);

// ---------- document ----------
const doc = new Document({
  creator: 'Student', title: 'CSA09 Assignment 2 Report',
  styles: {
    default: { document: { run: { font: 'Calibri', size: 22 } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: 'Calibri', size: 32, bold: true, color: '1F3864' }, paragraph: { spacing: { before: 240, after: 160 }, keepNext: true, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: 'Calibri', size: 26, bold: true, color: '2F5496' }, paragraph: { spacing: { before: 220, after: 100 }, keepNext: true, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: 'Calibri', size: 23, bold: true, color: '404040' }, paragraph: { spacing: { before: 160, after: 80 }, keepNext: true, outlineLevel: 2 } },
    ],
  },
  numbering: { config: [{ reference: 'bul', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1300, left: 1440 } } },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'CSA09 – Programming in Java – Assignment 2 (CO2)   |   Page ', size: 18 }), new TextRun({ children: [PageNumber.CURRENT], size: 18 })] })] }) },
    children: body,
  }],
});
Packer.toBuffer(doc).then(b => { fs.writeFileSync(path.join(ROOT, 'report/CSA09_Assignment2_Report.docx'), b); console.log('report written'); });
