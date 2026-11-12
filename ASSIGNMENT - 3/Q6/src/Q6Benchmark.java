import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Q6 alternative analysis: time per put/take and per transfer, and the space of each design. */
public class Q6Benchmark {
    static long sink;
    static double tokensWaitNotify(int n) throws Exception {
        BoundedCounter c = new BoundedCounter(5, true, true);
        Thread p = new Thread(() -> { try { for (int i = 0; i < n; i++) c.put(i & 127); } catch (InterruptedException e) { } });
        Thread[] cs = new Thread[2]; AtomicLong sum = new AtomicLong();
        for (int k = 0; k < 2; k++) cs[k] = new Thread(() -> { try { for (int i = 0; i < n / 2; i++) sum.addAndGet(c.take()); } catch (InterruptedException e) { } });
        long t0 = System.nanoTime(); p.start(); for (Thread t : cs) t.start(); p.join(); for (Thread t : cs) t.join();
        sink += sum.get(); return (System.nanoTime() - t0) / 1e6;
    }
    static double tokensQueue(int n) throws Exception {
        BlockingQueue<Integer> c = new ArrayBlockingQueue<>(5);
        Thread p = new Thread(() -> { try { for (int i = 0; i < n; i++) c.put(i & 127); } catch (InterruptedException e) { } });
        Thread[] cs = new Thread[2]; AtomicLong sum = new AtomicLong();
        for (int k = 0; k < 2; k++) cs[k] = new Thread(() -> { try { for (int i = 0; i < n / 2; i++) sum.addAndGet(c.take()); } catch (InterruptedException e) { } });
        long t0 = System.nanoTime(); p.start(); for (Thread t : cs) t.start(); p.join(); for (Thread t : cs) t.join();
        sink += sum.get(); return (System.nanoTime() - t0) / 1e6;
    }
    interface Transfer { void run(Wallet f, Wallet t, long a) throws Exception; }
    static double transfers(Transfer tr, int threads, int per, AtomicLong retries) throws Exception {
        Wallet[] w = new Wallet[6]; for (int i = 0; i < 6; i++) w[i] = new Wallet(i, 1_000_000);
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) ts[i] = new Thread(() -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            try { for (int k = 0; k < per; k++) { int f = r.nextInt(6), t = r.nextInt(6); if (f != t) tr.run(w[f], w[t], 1); } } catch (Exception e) { }
        });
        long t0 = System.nanoTime(); for (Thread t : ts) t.start(); for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / 1e6;
    }
    static long used() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. 1 producer + 2 consumers, capacity 5: time for N tokens (ms, best of 3) and per token (us) ===");
        System.out.printf("%-10s %-24s %-24s%n", "N", "wait()/notifyAll()", "ArrayBlockingQueue(5)");
        for (int n : new int[]{10_000, 50_000, 100_000}) {
            double a = 1e18, b = 1e18;
            for (int r = 0; r < 3; r++) { a = Math.min(a, tokensWaitNotify(n)); b = Math.min(b, tokensQueue(n)); }
            System.out.printf("%-10d %-8.0f ms (%.2f us)    %-8.0f ms (%.2f us)%n", n, a, a * 1000 / n, b, b * 1000 / n);
        }
        System.out.println();
        System.out.println("=== 2. 8 threads x 1,000 random transfers between 6 wallets, NO artificial pause (ms, best of 2; us per transfer) ===");
        double o = 1e18, t = 1e18; AtomicLong ret = new AtomicLong();
        for (int r = 0; r < 2; r++) {
            o = Math.min(o, transfers((f, to, m) -> Wallet.transferOrdered(f, to, m, 0), 8, 1_000, ret));
            t = Math.min(t, transfers((f, to, m) -> Wallet.transferTryLock(f, to, m, 0), 8, 1_000, ret));
        }
        int done = 8 * 1_000 * 5 / 6;
        System.out.printf("ordered synchronized locks : %.0f ms  (%.2f us per transfer)%n", o, o * 1000 / done);
        System.out.printf("tryLock(5 ms) + back-off  : %.0f ms  (%.2f us per transfer)%n", t, t * 1000 / done);
        System.out.println();
        System.out.println("=== 3. Space per counter object (bytes, 20,000 instances) ===");
        int K = 20_000;
        long m0 = used(); BoundedCounter[] bc = new BoundedCounter[K]; for (int i = 0; i < K; i++) bc[i] = new BoundedCounter(5, true, true);
        long m1 = used(); ArrayBlockingQueue<Integer>[] q = new ArrayBlockingQueue[K]; for (int i = 0; i < K; i++) q[i] = new ArrayBlockingQueue<>(5);
        long m2 = used(); Wallet[] ws = new Wallet[K]; for (int i = 0; i < K; i++) ws[i] = new Wallet(i, 0);
        long m3 = used();
        System.out.printf("wait/notify counter (int[5] + fields + trace list) : %d bytes%n", (m1 - m0) / K);
        System.out.printf("ArrayBlockingQueue(5) (Object[5] + lock + 2 conditions): %d bytes%n", (m2 - m1) / K);
        System.out.printf("Wallet (long + ReentrantLock): %d bytes per Wallet (includes its ReentrantLock)%n", (m3 - m2) / K);
        System.out.println("keep-alive " + (bc.length + q.length + ws.length + sink > 0));
    }
}
