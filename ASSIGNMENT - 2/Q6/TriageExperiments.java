import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Q6 - Emergency triage: correctness check, growth investigation, starvation and a fair rule,
 * and the two alternative designs (sorted LinkedList, five FIFO queues).
 * Uses the Patient class from Triage.java (compile both files together).
 */
public class TriageExperiments {

    static final int REPEATS = 5;
    static long sink = 0;

    static Comparator<Patient> severityThenArrival() {
        return Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival);
    }

    // ---------------- fair rule: aging ----------------
    // every WAIT_LIMIT time units of waiting lowers the severity number by one (never below 1)
    static final int WAIT_LIMIT = 50;

    static int effective(Patient p, long now) {
        int e = p.severity - (int) ((now - p.arrival) / WAIT_LIMIT);
        return Math.max(1, e);
    }

    // Fair triage, simple version: because priorities change with time, a normal PriorityQueue
    // would be in the wrong order, so the queue is REBUILT with the current time before every serve.
    static class FairTriage {
        PriorityQueue<Patient> queue = new PriorityQueue<>(severityThenArrival());
        void admit(Patient p) { queue.add(p); }
        Patient serve(final long now) {
            Comparator<Patient> byEffective = Comparator.comparingInt((Patient p) -> effective(p, now))
                                                        .thenComparingLong(p -> p.arrival);
            PriorityQueue<Patient> fresh = new PriorityQueue<>(byEffective);
            fresh.addAll(queue);                       // re-examines every waiting patient
            queue = fresh;
            return queue.poll();
        }
        int size() { return queue.size(); }
    }

    // ---------------- alternative 1 : sorted LinkedList ----------------
    static class SortedListTriage {
        LinkedList<Patient> list = new LinkedList<>();
        void admit(Patient np) {
            ListIterator<Patient> it = list.listIterator();
            while (it.hasNext()) {
                Patient p = it.next();
                if (p.severity > np.severity) { it.previous(); break; }   // go back one step and insert before
            }
            it.add(np);
        }
        Patient next() { return list.pollFirst(); }
    }

    // ---------------- alternative 2 : five FIFO queues ----------------
    static class FiveQueueTriage {
        @SuppressWarnings("unchecked")
        ArrayDeque<Patient>[] q = new ArrayDeque[5];
        FiveQueueTriage() { for (int i = 0; i < 5; i++) q[i] = new ArrayDeque<>(); }
        void admit(Patient p) { q[p.severity - 1].addLast(p); }
        Patient next() {
            for (int i = 0; i < 5; i++) if (!q[i].isEmpty()) return q[i].pollFirst();
            return null;
        }
    }

    static Patient[] makePatients(int n, long seed) {
        Random rnd = new Random(seed);
        Patient[] ps = new Patient[n];
        for (int i = 0; i < n; i++) ps[i] = new Patient("P" + i, 1 + rnd.nextInt(5), i);
        return ps;
    }

    public static void main(String[] args) throws Exception {
        // =============== 1. hand verification with 8 patients ===============
        System.out.println("=== 1. Hand-checked instance (8 patients) ===");
        String[] names = {"Arjun", "Bala", "Chitra", "Devi", "Esha", "Farid", "Gita", "Hari"};
        int[] sev =      {3,       2,      5,        1,      3,      2,       1,      4};
        String expected = "Devi Gita Bala Farid Arjun Esha Hari Chitra";   // worked out by hand: S1, S2, S3, S4, S5, each in arrival order

        PriorityQueue<Patient> pq = new PriorityQueue<>(severityThenArrival());
        ArrayDeque<Patient> plain = new ArrayDeque<>();
        PriorityQueue<Patient> noArrival = new PriorityQueue<>(Comparator.comparingInt((Patient p) -> p.severity));
        for (int i = 0; i < names.length; i++) {
            Patient p = new Patient(names[i], sev[i], i);
            pq.add(p); plain.add(p); noArrival.add(p);
        }
        System.out.println("Arrival order (plain FIFO queue)       : " + plain);
        System.out.println("System.out.println(priorityQueue)      : " + pq + "   <- heap array order, NOT treatment order");
        PriorityQueue<Patient> copy = new PriorityQueue<>(pq);
        StringBuilder sb = new StringBuilder();
        while (!copy.isEmpty()) sb.append(copy.poll().name).append(' ');
        System.out.println("Treatment order using poll()           : " + sb.toString().trim());
        System.out.println("Hand-worked expected order             : " + expected);
        System.out.println("Program matches hand calculation       : " + sb.toString().trim().equals(expected));
        StringBuilder sb2 = new StringBuilder();
        while (!noArrival.isEmpty()) sb2.append(noArrival.poll()).append(' ');
        System.out.println("Comparator WITHOUT arrival counter     : " + sb2.toString().trim() + "   <- equal severities not guaranteed in arrival order");

        // =============== 2. growth investigation ===============
        System.out.println();
        System.out.println("=== 2. Growth investigation: PriorityQueue cost per operation (average of 30 runs, 5 for n=1,000,000) ===");
        System.out.println("PREDICTION: add = O(log n), poll = O(log n); going from 1,000 to 1,000,000 patients (x1000) should raise the cost per");
        System.out.println("            operation only slightly (log2 grows from 10 to 20, i.e. about x2) plus some cache effects.");
        PrintWriter g = new PrintWriter("q6_growth_results.csv");
        g.println("n,add_ns_per_op,poll_ns_per_op,total_add_ms,total_poll_ms");
        System.out.printf("%10s %16s %16s %14s %14s%n", "n", "add ns/op", "poll ns/op", "all adds ms", "all polls ms");
        for (int w = 0; w < 10; w++) { runPQ(makePatients(1000, w)); runPQ(makePatients(10000, w)); runPQ(makePatients(100000, w)); }
        for (int n : new int[]{1000, 10000, 100000, 1000000}) {
            Patient[] ps = makePatients(n, 11);
            double add = 0, poll = 0;
            int reps = (n >= 1000000) ? 5 : 30;
            for (int r = 0; r < reps; r++) { double[] t = runPQ(ps); add += t[0]; poll += t[1]; }
            add /= reps; poll /= reps;
            System.out.printf("%10d %16.1f %16.1f %14.2f %14.2f%n", n, add * 1e6 / n, poll * 1e6 / n, add, poll);
            g.printf("%d,%.1f,%.1f,%.3f,%.3f%n", n, add * 1e6 / n, poll * 1e6 / n, add, poll);
        }
        g.close();

        // =============== 3. starvation and fair rule ===============
        System.out.println();
        System.out.println("=== 3. Starvation and the aging (fair) rule ===");
        System.out.println("Scenario: a severity-5 patient arrives at time 0. At every time unit one new patient (severity 1) arrives and");
        System.out.println("one patient is served. The queue starts with 10 waiting severity-1 patients, so it never becomes empty.");
        System.out.println("(worst case: every new arrival is severity 1, i.e. only critical patients keep arriving)");
        long ticks = 2000;
        // (a) plain priority queue
        PriorityQueue<Patient> plainPQ = new PriorityQueue<>(severityThenArrival());
        Random rnd = new Random(3);
        long id = 0;
        Patient old = new Patient("OldPatient", 5, id++);
        plainPQ.add(old);
        for (int i = 0; i < 10; i++) plainPQ.add(new Patient("Init" + i, 1, id++));
        long servedAtPlain = -1;
        for (long t = 1; t <= ticks; t++) {
            plainPQ.add(new Patient("N" + t, 1, id++));
            Patient p = plainPQ.poll();
            if (p == old) { servedAtPlain = t; break; }
        }
        System.out.println("Plain PriorityQueue : severity-5 patient served at time " + (servedAtPlain < 0 ? "NEVER (still waiting after " + ticks + " time units)" : servedAtPlain));
        // (b) fair rule (same arrivals)
        FairTriage fair = new FairTriage();
        rnd = new Random(3);
        id = 0;
        Patient old2 = new Patient("OldPatient", 5, id++);
        fair.admit(old2);
        for (int i = 0; i < 10; i++) fair.admit(new Patient("Init" + i, 1, id++));
        long servedAtFair = -1;
        for (long t = 1; t <= ticks; t++) {
            fair.admit(new Patient("N" + t, 1, id++));
            Patient p = fair.serve(t);
            if (p == old2) { servedAtFair = t; break; }
        }
        System.out.println("Fair rule (aging, limit " + WAIT_LIMIT + ") : severity-5 patient served at time " + servedAtFair
                + "   (expected: 4 x " + WAIT_LIMIT + " = " + (4 * WAIT_LIMIT) + " units, when his effective severity has dropped from 5 to 1 and his earlier arrival wins)");

        // =============== 4. deployment decision ===============
        System.out.println();
        System.out.println("=== 4. Deployment: a city hospital with 5,000 patients a day (worst case: all 5,000 waiting together) ===");
        Patient[] day = makePatients(5000, 21);
        double pqMs = 0, fairMs = 0;
        for (int r = 0; r < REPEATS; r++) {
            long s = System.nanoTime();
            PriorityQueue<Patient> q = new PriorityQueue<>(severityThenArrival());
            for (Patient p : day) q.add(p);
            while (!q.isEmpty()) sink += q.poll().severity;
            pqMs += (System.nanoTime() - s) / 1e6;

            s = System.nanoTime();
            FairTriage f = new FairTriage();
            for (Patient p : day) f.admit(p);
            for (long t = 0; t < 5000; t++) sink += f.serve(t).severity;
            fairMs += (System.nanoTime() - s) / 1e6;
        }
        System.out.printf("Plain PriorityQueue : admit + serve 5,000 patients = %.2f ms in total (%.3f microseconds per patient)%n", pqMs / REPEATS, pqMs / REPEATS * 1000 / 5000);
        System.out.printf("Fair (rebuild) rule : admit + serve 5,000 patients = %.2f ms in total (%.3f microseconds per patient)%n", fairMs / REPEATS, fairMs / REPEATS * 1000 / 5000);

        // =============== 5. alternative designs ===============
        System.out.println();
        System.out.println("=== 5. Alternative designs: admit n patients, then serve all n (average of " + REPEATS + " runs, ms) ===");
        System.out.println("PREDICTION: PriorityQueue O(log n) each; sorted LinkedList admit O(n) (serve O(1)); five queues O(1) each.");
        PrintWriter a = new PrintWriter("q6_alternatives_results.csv");
        a.println("n,design,admit_all_ms,serve_all_ms");
        System.out.printf("%8s %-22s %14s %14s%n", "n", "Design", "admit all", "serve all");
        int[] altSizes = {5000, 1000, 5000, 20000};                    // the first 5000 is only a warm-up
        for (int si = 0; si < altSizes.length; si++) {
            int n = altSizes[si];
            boolean warm = (si == 0);
            Patient[] ps = makePatients(n, 5);
            for (int d = 0; d < 3; d++) {
                double ad = 0, sv = 0;
                for (int r = 0; r < REPEATS; r++) {
                    long s = System.nanoTime();
                    double admitMs, serveMs;
                    if (d == 0) {
                        PriorityQueue<Patient> q = new PriorityQueue<>(severityThenArrival());
                        for (Patient p : ps) q.add(p);
                        admitMs = (System.nanoTime() - s) / 1e6;
                        s = System.nanoTime();
                        while (!q.isEmpty()) sink += q.poll().severity;
                        serveMs = (System.nanoTime() - s) / 1e6;
                    } else if (d == 1) {
                        SortedListTriage q = new SortedListTriage();
                        for (Patient p : ps) q.admit(p);
                        admitMs = (System.nanoTime() - s) / 1e6;
                        s = System.nanoTime();
                        Patient p;
                        while ((p = q.next()) != null) sink += p.severity;
                        serveMs = (System.nanoTime() - s) / 1e6;
                    } else {
                        FiveQueueTriage q = new FiveQueueTriage();
                        for (Patient p : ps) q.admit(p);
                        admitMs = (System.nanoTime() - s) / 1e6;
                        s = System.nanoTime();
                        Patient p;
                        while ((p = q.next()) != null) sink += p.severity;
                        serveMs = (System.nanoTime() - s) / 1e6;
                    }
                    ad += admitMs; sv += serveMs;
                }
                String dn = d == 0 ? "PriorityQueue" : d == 1 ? "Sorted LinkedList" : "Five FIFO queues";
                if (warm) continue;                                       // warm-up results are thrown away
                System.out.printf("%8d %-22s %14.3f %14.3f%n", n, dn, ad / REPEATS, sv / REPEATS);
                a.printf("%d,%s,%.3f,%.3f%n", n, dn, ad / REPEATS, sv / REPEATS);
            }
        }
        a.close();
        System.out.println();
        System.out.println("(checksum " + sink + ")");
    }

    // returns {time to add all, time to poll all} in ms
    static double[] runPQ(Patient[] ps) {
        PriorityQueue<Patient> q = new PriorityQueue<>(severityThenArrival());
        long s = System.nanoTime();
        for (Patient p : ps) q.add(p);
        double add = (System.nanoTime() - s) / 1e6;
        s = System.nanoTime();
        while (!q.isEmpty()) sink += q.poll().severity;
        double poll = (System.nanoTime() - s) / 1e6;
        return new double[]{add, poll};
    }
}
