import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Q7 - Diagnose, redesign and validate.
 * Chosen solution: the FAIR emergency triage queue from Q6 (aging rule).
 *
 *   ORIGINAL  : PriorityQueue that is rebuilt before every serve (priorities change with time).
 *   REDESIGN  : five FIFO queues (one per severity). Only the HEAD of each queue is compared at serve time.
 *
 * Aging rule: effective severity = max(1, severity - waited / waitLimit)
 * Order of service: lowest effective severity first, then earliest arrival (seq).
 */
public class TriageFair {

    static class Patient {
        final String name;
        final int severity;
        final long seq;        // arrival number (tie-break)
        final long time;       // arrival time (used for aging), never decreases with seq
        Patient(String name, int severity, long seq, long time) {
            this.name = name; this.severity = severity; this.seq = seq; this.time = time;
        }
        public String toString() { return name + "(S" + severity + ")"; }
    }

    static int effective(Patient p, long now, long waitLimit) {
        return Math.max(1, p.severity - (int) ((now - p.time) / waitLimit));
    }

    // =============== ORIGINAL design ===============
    static class RebuildTriage {
        PriorityQueue<Patient> queue = new PriorityQueue<>(Comparator.comparingLong((Patient p) -> p.seq));
        final long waitLimit;
        long examined = 0;         // number of patients re-examined (operation count)
        long queuesCreated = 0;    // number of PriorityQueue objects created

        RebuildTriage(long waitLimit) { this.waitLimit = waitLimit; }
        void admit(Patient p) { queue.add(p); }
        Patient serve(final long now) {
            Comparator<Patient> cmp = Comparator.comparingInt((Patient p) -> effective(p, now, waitLimit))
                                                .thenComparingLong(p -> p.seq);
            PriorityQueue<Patient> fresh = new PriorityQueue<>(cmp);
            queuesCreated++;
            examined += queue.size();
            fresh.addAll(queue);
            queue = fresh;
            return queue.poll();
        }
    }

    // =============== REDESIGN ===============
    static class FiveQueueTriage {
        @SuppressWarnings("unchecked")
        final ArrayDeque<Patient>[] q = new ArrayDeque[5];
        final long waitLimit;
        long examined = 0;         // number of queue heads compared

        FiveQueueTriage(long waitLimit) {
            this.waitLimit = waitLimit;
            for (int i = 0; i < 5; i++) q[i] = new ArrayDeque<>();
        }
        void admit(Patient p) { q[p.severity - 1].addLast(p); }
        Patient serve(long now) {
            Patient best = null;
            int bestLevel = -1;
            for (int i = 0; i < 5; i++) {
                Patient head = q[i].peekFirst();       // oldest patient of this level = best of this level
                if (head == null) continue;
                examined++;
                if (best == null) { best = head; bestLevel = i; continue; }
                int e1 = effective(head, now, waitLimit), e2 = effective(best, now, waitLimit);
                if (e1 < e2 || (e1 == e2 && head.seq < best.seq)) { best = head; bestLevel = i; }
            }
            if (best == null) return null;
            return q[bestLevel].pollFirst();
        }
    }

    // =============== helpers ===============
    static long allocated() {
        com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        return b.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    static Patient[] makePatients(int n, long seed) {
        Random rnd = new Random(seed);
        Patient[] ps = new Patient[n];
        for (int i = 0; i < n; i++) ps[i] = new Patient("P" + i, 1 + rnd.nextInt(5), i, i / 100);   // 100 arrivals per time unit
        return ps;
    }

    // Workload: all n patients are waiting, then served one by one; the doctors serve 10 patients per time unit
    static long checksumOrder = 0;
    static List<String> drainRebuild(Patient[] ps, long waitLimit, long[] counters) {
        RebuildTriage t = new RebuildTriage(waitLimit);
        for (Patient p : ps) t.admit(p);
        long start = ps[ps.length - 1].time;
        List<String> order = new ArrayList<>();
        for (int i = 0; i < ps.length; i++) order.add(t.serve(start + i / 10).name);
        counters[0] = t.examined; counters[1] = t.queuesCreated;
        return order;
    }
    static List<String> drainFive(Patient[] ps, long waitLimit, long[] counters) {
        FiveQueueTriage t = new FiveQueueTriage(waitLimit);
        for (Patient p : ps) t.admit(p);
        long start = ps[ps.length - 1].time;
        List<String> order = new ArrayList<>();
        for (int i = 0; i < ps.length; i++) order.add(t.serve(start + i / 10).name);
        counters[0] = t.examined; counters[1] = 0;
        return order;
    }

    public static void main(String[] args) throws Exception {
        final long WAIT_LIMIT = 500;

        // ---------------- A. Equivalence test on random mixed admit / serve sequences ----------------
        System.out.println("=== E1. Correctness: do both designs serve patients in exactly the same order? ===");
        Random rnd = new Random(2024);
        int trials = 300, mismatches = 0, servedTotal = 0;
        for (int trial = 0; trial < trials; trial++) {
            long wl = 1 + rnd.nextInt(8);                       // small limits so that aging happens a lot
            RebuildTriage a = new RebuildTriage(wl);
            FiveQueueTriage b = new FiveQueueTriage(wl);
            long now = 0, seq = 0;
            for (int step = 0; step < 400; step++) {
                if (rnd.nextInt(3) < 2) {                       // admit (about 2 out of 3 steps)
                    Patient p = new Patient("P" + seq, 1 + rnd.nextInt(5), seq, now);
                    seq++;
                    a.admit(p); b.admit(p);
                } else {                                        // serve
                    Patient x = a.serve(now), y = b.serve(now);
                    servedTotal++;
                    if (x != y) mismatches++;                   // same Patient object expected (or both null)
                }
                if (rnd.nextInt(4) == 0) now++;                 // time moves on
            }
        }
        System.out.println("Random trials: " + trials + ", patients served and compared: " + servedTotal + ", mismatches: " + mismatches);

        // small readable example
        Patient[] demo = {
            new Patient("Ravi", 3, 0, 0), new Patient("Meena", 1, 1, 0), new Patient("Asha", 5, 2, 0),
            new Patient("Kumar", 1, 3, 1), new Patient("Lata", 2, 4, 2), new Patient("John", 3, 5, 2)};
        RebuildTriage d1 = new RebuildTriage(2);
        FiveQueueTriage d2 = new FiveQueueTriage(2);
        for (Patient p : demo) { d1.admit(p); d2.admit(p); }
        StringBuilder s1 = new StringBuilder(), s2 = new StringBuilder();
        for (long now = 3; now < 9; now++) { s1.append(d1.serve(now)).append(' '); s2.append(d2.serve(now)).append(' '); }
        System.out.println("Demo (wait limit 2) original : " + s1.toString().trim());
        System.out.println("Demo (wait limit 2) redesign : " + s2.toString().trim());

        // ---------------- B. Time, operations and memory ----------------
        System.out.println();
        System.out.println("=== E2. Original vs redesign: serve all n waiting patients (wait limit " + WAIT_LIMIT + ", 10 serves per time unit) ===");
        PrintWriter csv = new PrintWriter("q7_results.csv");
        csv.println("n,design,avg_time_ms,patients_or_heads_examined,priority_queues_created,allocated_MB,same_order");
        System.out.printf("%8s %-22s %12s %18s %12s %12s %10s%n", "n", "Design", "time (ms)", "examined (ops)", "PQs created", "alloc (MB)", "same order");

        // warm-up
        for (int w = 0; w < 3; w++) {
            long[] c = new long[2];
            drainRebuild(makePatients(2000, w), WAIT_LIMIT, c);
            drainFive(makePatients(2000, w), WAIT_LIMIT, c);
        }
        int[] sizes = {1000, 2500, 5000, 10000, 20000};
        for (int n : sizes) {
            Patient[] ps = makePatients(n, 77);
            int reps = (n >= 10000) ? 3 : 5;
            double[] ms = new double[2];
            double[] mb = new double[2];
            long[][] cnt = new long[2][2];
            List<String> o1 = null, o2 = null;
            for (int d = 0; d < 2; d++) {
                double total = 0;
                for (int r = 0; r < reps; r++) {
                    long a0 = allocated();
                    long st = System.nanoTime();
                    List<String> o = (d == 0) ? drainRebuild(ps, WAIT_LIMIT, cnt[d]) : drainFive(ps, WAIT_LIMIT, cnt[d]);
                    total += (System.nanoTime() - st) / 1e6;
                    if (r == 0) mb[d] = (allocated() - a0) / 1048576.0;
                    if (d == 0) o1 = o; else o2 = o;
                }
                ms[d] = total / reps;
            }
            boolean same = o1.equals(o2);
            System.out.printf("%8d %-22s %12.2f %18d %12d %12.1f %10b%n", n, "Original (rebuild PQ)", ms[0], cnt[0][0], cnt[0][1], mb[0], same);
            System.out.printf("%8d %-22s %12.2f %18d %12d %12.1f %10b%n", n, "Redesign (5 queues)", ms[1], cnt[1][0], cnt[1][1], mb[1], same);
            csv.printf("%d,Original (rebuild PQ),%.3f,%d,%d,%.2f,%b%n", n, ms[0], cnt[0][0], cnt[0][1], mb[0], same);
            csv.printf("%d,Redesign (5 queues),%.3f,%d,%d,%.2f,%b%n", n, ms[1], cnt[1][0], cnt[1][1], mb[1], same);
            System.out.printf("%8s speed-up of redesign = %.1f times%n", "", ms[0] / ms[1]);
        }

        // redesign alone on bigger sizes
        System.out.println();
        System.out.println("=== E3. Redesign alone on larger queues (original would take too long) ===");
        PrintWriter big = new PrintWriter("q7_redesign_large.csv");
        big.println("n,avg_time_ms,heads_examined,ns_per_serve");
        System.out.printf("%10s %12s %16s %14s%n", "n", "time (ms)", "heads examined", "ns per serve");
        for (int n : new int[]{100000, 1000000}) {
            Patient[] ps = makePatients(n, 78);
            double total = 0; long[] c = new long[2];
            for (int r = 0; r < 3; r++) {
                long st = System.nanoTime();
                drainFive(ps, WAIT_LIMIT, c);
                total += (System.nanoTime() - st) / 1e6;
            }
            double ms = total / 3;
            System.out.printf("%10d %12.2f %16d %14.1f%n", n, ms, c[0], ms * 1e6 / n);
            big.printf("%d,%.3f,%d,%.1f%n", n, ms, c[0], ms * 1e6 / n);
        }
        big.close();
        csv.close();
    }
}
