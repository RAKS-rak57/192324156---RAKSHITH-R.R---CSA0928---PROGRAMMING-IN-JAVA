import java.util.*;

/**
 * Alternative concept: error codes instead of exceptions.
 * Measures (1) parse-with-exception vs parse-with-code at several failure rates,
 *          (2) cost of THROWING at different stack depths, (3) memory kept per exception object.
 */
public class ExceptionCostBenchmark {
    static int sink = 0;

    // ---- exception style --------------------------------------------------
    static int parseThrowing(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return -1; }
    }
    // ---- error-code style: check first (what hasNextInt() does for a Scanner) ----
    static boolean looksLikeInt(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c < '0' || c > '9') return false; }
        return true;
    }
    static int parseCode(String s) {
        if (!looksLikeInt(s)) return -1;
        return Integer.parseInt(s);
    }

    // ---- throwing from a given depth -----------------------------------------
    static class NoTrace extends Exception {          // exception whose stack trace is NOT captured
        NoTrace() { super("x", null, false, false); }
    }
    static final NoTrace PREALLOCATED = new NoTrace();
    static int throwFromDepth(int depth, int kind) throws Exception {
        if (depth > 0) return throwFromDepth(depth - 1, kind) + 1;
        switch (kind) {
            case 0: throw new Exception("normal");            // fills in the stack trace
            case 1: throw new NoTrace();                      // new object, no stack trace
            default: throw PREALLOCATED;                      // re-used object
        }
    }
    static Exception makeAtDepth(int depth) {
        if (depth > 0) return makeAtDepth(depth - 1);
        return new Exception("e");
    }
    static long used() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Parsing 1,000,000 inputs: exception vs error code (ns per input, best of 5) ===");
        System.out.printf("%-14s %-16s %-16s%n", "bad inputs", "try/catch", "error code");
        int N = 1_000_000;
        for (int pct : new int[]{0, 1, 10, 50, 100}) {
            String[] in = new String[N];
            Random rnd = new Random(1);
            for (int i = 0; i < N; i++) in[i] = rnd.nextInt(100) < pct ? "abc" : "1234";
            double bestA = 1e18, bestB = 1e18;
            for (int rep = 0; rep < 5; rep++) {
                long t0 = System.nanoTime(); int s = 0;
                for (int i = 0; i < N; i++) s += parseThrowing(in[i]);
                long t1 = System.nanoTime();
                for (int i = 0; i < N; i++) s += parseCode(in[i]);
                long t2 = System.nanoTime();
                sink += s;
                bestA = Math.min(bestA, (t1 - t0) / (double) N); bestB = Math.min(bestB, (t2 - t1) / (double) N);
            }
            System.out.printf("%-14s %-16.1f %-16.1f%n", pct + " %", bestA, bestB);
        }

        System.out.println();
        System.out.println("=== 2. Cost of one throw + catch by stack depth (ns, best of 5) ===");
        System.out.printf("%-8s %-22s %-24s %-22s%n", "depth", "new Exception()", "new, no stack trace", "pre-allocated");
        for (int depth : new int[]{1, 10, 50, 200, 500}) {
            int reps = 20_000;
            double[] best = {1e18, 1e18, 1e18};
            for (int round = 0; round < 5; round++) {
                for (int kind = 0; kind < 3; kind++) {
                    long t0 = System.nanoTime();
                    for (int i = 0; i < reps; i++) { try { sink += throwFromDepth(depth, kind); } catch (Exception e) { sink++; } }
                    best[kind] = Math.min(best[kind], (System.nanoTime() - t0) / (double) reps);
                }
            }
            System.out.printf("%-8d %-22.0f %-24.0f %-22.0f%n", depth, best[0], best[1], best[2]);
        }

        System.out.println();
        System.out.println("=== 3. Memory held by one exception object (bytes, 50,000 kept alive) ===");
        System.out.printf("%-8s %-26s %-26s%n", "depth", "trace captured only", "after getStackTrace()");
        for (int depth : new int[]{1, 10, 100}) {
            int K = 50_000;
            long m0 = used();
            Exception[] keep = new Exception[K];
            for (int i = 0; i < K; i++) keep[i] = makeAtDepth(depth);
            long m1 = used();
            for (int i = 0; i < K; i++) sink += keep[i].getStackTrace().length;
            long m2 = used();
            System.out.printf("%-8d %-26d %-26d%n", depth, (m1 - m0) / K, (m2 - m0) / K);
            sink += keep.length;
        }
        System.out.println("(sink=" + (sink != 0) + ")");
    }
}
