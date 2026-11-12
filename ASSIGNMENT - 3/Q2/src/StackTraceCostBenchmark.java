import java.io.IOException;
import portal.SubmissionStorageException;

/** Q2 alternative analysis: cost of capturing a stack trace per exception, and memory held by a chained exception. */
public class StackTraceCostBenchmark {
    static int sink;
    static Exception single(int depth) { return depth > 0 ? single(depth - 1) : new Exception("x"); }
    static Exception chained(int depth) {
        if (depth > 0) return chained(depth - 1);
        return new SubmissionStorageException("Could not store", new IOException("Disk quota exceeded"));   // TWO stack traces captured
    }
    static long used() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    public static void main(String[] args) {
        System.out.println("=== Time to CREATE (not throw) one exception object, ns, best of 5 ===");
        System.out.printf("%-8s %-22s %-26s%n", "depth", "single exception", "wrapper + cause (chained)");
        for (int depth : new int[]{5, 25, 100, 400}) {
            int reps = 30_000; double b1 = 1e18, b2 = 1e18;
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime();
                for (int i = 0; i < reps; i++) sink += single(depth).hashCode();
                long t1 = System.nanoTime();
                for (int i = 0; i < reps; i++) sink += chained(depth).hashCode();
                long t2 = System.nanoTime();
                b1 = Math.min(b1, (t1 - t0) / (double) reps); b2 = Math.min(b2, (t2 - t1) / (double) reps);
            }
            System.out.printf("%-8d %-22.0f %-26.0f%n", depth, b1, b2);
        }
        System.out.println();
        System.out.println("=== Memory kept alive per exception object, bytes (30,000 objects, trace not yet expanded) ===");
        System.out.printf("%-8s %-22s %-26s%n", "depth", "single exception", "wrapper + cause (chained)");
        for (int depth : new int[]{5, 25, 100}) {
            int K = 30_000;
            long m0 = used(); Exception[] a = new Exception[K];
            for (int i = 0; i < K; i++) a[i] = single(depth);
            long m1 = used(); Exception[] b = new Exception[K];
            for (int i = 0; i < K; i++) b[i] = chained(depth);
            long m2 = used();
            System.out.printf("%-8d %-22d %-26d%n", depth, (m1 - m0) / K, (m2 - m1) / K);
            sink += a.length + b.length;
        }
        System.out.println("(sink=" + (sink != 0) + ")");
    }
}
