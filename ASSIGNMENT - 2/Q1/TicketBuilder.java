import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.StringJoiner;

/**
 * Q1 - Building text under load: String vs StringBuilder vs StringBuffer
 * Railway e-ticket generation.
 * Approaches:
 *   0 = String with + in a loop
 *   1 = StringBuilder
 *   2 = StringBuffer
 *   3 = String.join()      (alternative concept)
 *   4 = StringJoiner       (alternative concept)
 */
public class TicketBuilder {

    static final String[] NAMES = {"String +", "StringBuilder", "StringBuffer", "String.join", "StringJoiner"};
    static final int REPEATS = 5;      // number of measured runs that are averaged
    static long checksum = 0;          // used so the JIT cannot throw the work away

    // ---------- the five ways of building one ticket ----------
    static String[] makeFields(int n) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) {
            f[i] = "FIELD" + i + "=VALUE" + (i % 97);
        }
        return f;
    }

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
    }

    static String buildStringBuffer(String[] fields) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i]).append("|");
        }
        return sb.toString();
    }

    static String buildJoin(String[] fields) {
        return String.join("|", fields);
    }

    static String buildJoiner(String[] fields) {
        StringJoiner sj = new StringJoiner("|");
        for (int i = 0; i < fields.length; i++) {
            sj.add(fields[i]);
        }
        return sj.toString();
    }

    static String build(int approach, String[] fields) {
        switch (approach) {
            case 0: return buildPlus(fields);
            case 1: return buildStringBuilder(fields);
            case 2: return buildStringBuffer(fields);
            case 3: return buildJoin(fields);
            default: return buildJoiner(fields);
        }
    }

    // ---------- measuring helpers ----------
    static long allocatedBytes() {
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        return bean.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    static long usedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    static void runTickets(int approach, int tickets, String[] fields) {
        for (int t = 0; t < tickets; t++) {
            checksum += build(approach, fields).length();
        }
    }

    // average time in milliseconds over REPEATS runs
    static double averageMs(int approach, int tickets, String[] fields) {
        long total = 0;
        for (int r = 0; r < REPEATS; r++) {
            long start = System.nanoTime();
            runTickets(approach, tickets, fields);
            total += System.nanoTime() - start;
        }
        return total / (double) REPEATS / 1_000_000.0;
    }

    // estimated number of String objects created for the whole workload
    static long estimateObjects(int approach, int tickets, int fields) {
        if (approach == 0) return (long) tickets * fields;          // one new String per + step
        if (approach == 1 || approach == 2) return (long) tickets * 2; // builder + final toString
        return (long) tickets * 2;                                  // join/joiner: a few objects only
    }

    // estimated characters copied for ONE ticket
    static long estimateCharsCopied(int approach, String[] fields) {
        long len = 0, copied = 0;
        for (int i = 0; i < fields.length; i++) {
            len += fields[i].length() + 1;
            copied += len;                       // + copies the whole string built so far
        }
        if (approach == 0) return copied;
        return 3 * len;                          // append once + growth copies + toString
    }

    public static void main(String[] args) throws Exception {
        // ---------- warm-up so the JIT compiler has finished its work ----------
        String[] warm = makeFields(50);
        for (int a = 0; a < 5; a++) {
            for (int i = 0; i < 3; i++) runTickets(a, 2000, warm);
        }

        PrintWriter csv = new PrintWriter("q1_workloads.csv");
        csv.println("workload,tickets,fields,approach,avg_time_ms,allocated_MB,used_before_MB,used_after_MB,est_objects,est_chars_copied_per_ticket");

        int[][] workloads = {{1000, 10}, {10000, 50}, {20, 5000}};
        String[] wname = {"W1 short tickets", "W2 normal tickets", "W3 group booking"};

        System.out.println("=== Q1 : Measured workloads (average of " + REPEATS + " runs) ===");
        for (int w = 0; w < workloads.length; w++) {
            int tickets = workloads[w][0];
            int n = workloads[w][1];
            String[] fields = makeFields(n);
            System.out.println();
            System.out.println(wname[w] + " : " + tickets + " tickets x " + n + " fields");
            System.out.printf("%-14s %12s %14s %14s %14s %16s%n", "Approach", "Time (ms)", "Alloc (MB)", "Used before", "Used after", "Est. Strings");
            for (int a = 0; a < 5; a++) {
                System.gc();
                long before = usedMemory();
                long allocBefore = allocatedBytes();
                runTickets(a, tickets, fields);                    // one run just for memory
                long allocAfter = allocatedBytes();
                long after = usedMemory();
                double ms = averageMs(a, tickets, fields);
                double allocMB = (allocAfter - allocBefore) / 1048576.0;
                long objs = estimateObjects(a, tickets, n);
                long copied = estimateCharsCopied(a, fields);
                System.out.printf("%-14s %12.3f %14.2f %11.1f MB %11.1f MB %16d%n",
                        NAMES[a], ms, allocMB, before / 1048576.0, after / 1048576.0, objs);
                csv.printf("%s,%d,%d,%s,%.3f,%.2f,%.1f,%.1f,%d,%d%n",
                        wname[w], tickets, n, NAMES[a], ms, allocMB, before / 1048576.0, after / 1048576.0, objs, copied);
            }
        }
        csv.close();

        // ---------- scaling: one ticket, growing number of fields ----------
        PrintWriter sc = new PrintWriter("q1_scaling.csv");
        sc.println("fields,String +,StringBuilder,StringBuffer,String.join,StringJoiner");
        System.out.println();
        System.out.println("=== Scaling: time (ms) to build ONE ticket with n fields (avg of " + REPEATS + ") ===");
        System.out.printf("%8s %12s %14s %14s %12s %12s%n", "n", "String +", "StringBuilder", "StringBuffer", "join", "joiner");
        for (int n = 500; n <= 32000; n *= 2) {
            String[] fields = makeFields(n);
            double[] t = new double[5];
            for (int a = 0; a < 5; a++) t[a] = averageMs(a, 1, fields);
            System.out.printf("%8d %12.3f %14.3f %14.3f %12.3f %12.3f%n", n, t[0], t[1], t[2], t[3], t[4]);
            sc.printf("%d,%.4f,%.4f,%.4f,%.4f,%.4f%n", n, t[0], t[1], t[2], t[3], t[4]);
        }
        sc.close();

        // ---------- design decision: shared booking log with several threads ----------
        System.out.println();
        System.out.println("=== Shared booking log: 4 threads x 50,000 appends (expected length = 4 x 50000 x 6) ===");
        PrintWriter th = new PrintWriter("q1_threads.csv");
        th.println("trial,class,expected_length,actual_length,exceptions");
        final int THREADS = 4, APPENDS = 50000;
        long expected = (long) THREADS * APPENDS * 6;   // "BOOK1\n" has 6 characters
        for (int trial = 1; trial <= 20; trial++) {
            for (int kind = 0; kind < 2; kind++) {
                final StringBuilder unsafe = new StringBuilder();
                final StringBuffer safe = new StringBuffer();
                final int[] errors = {0};
                Thread[] ts = new Thread[THREADS];
                for (int i = 0; i < THREADS; i++) {
                    final int k = kind;
                    ts[i] = new Thread(() -> {
                        for (int j = 0; j < APPENDS; j++) {
                            try {
                                if (k == 0) unsafe.append("BOOK1\n");
                                else safe.append("BOOK1\n");
                            } catch (Exception e) {
                                synchronized (errors) { errors[0]++; }
                            }
                        }
                    });
                    ts[i].start();
                }
                for (int i = 0; i < THREADS; i++) ts[i].join();
                String cls = (kind == 0) ? "StringBuilder" : "StringBuffer";
                long actual = (kind == 0) ? unsafe.length() : safe.length();
                System.out.printf("Trial %d  %-14s expected=%d actual=%d exceptions=%d%n", trial, cls, expected, actual, errors[0]);
                th.printf("%d,%s,%d,%d,%d%n", trial, cls, expected, actual, errors[0]);
            }
        }
        th.close();
        System.out.println();
        System.out.println("(checksum " + checksum + " - printed only so the work is not optimised away)");
    }
}
