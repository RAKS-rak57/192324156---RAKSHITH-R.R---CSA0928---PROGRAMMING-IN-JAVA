import java.util.*;
import java.util.concurrent.*;

/** Student B: time of 10,000 bookings, lock scope, contention, and extra space for the alternative strategies. */
public class BookingBenchmark {
    interface Maker { Bookers.Booker make(int seats, int work); }
    static final String[] NAMES = {"synchronized method", "synchronized block", "ReentrantLock", "AtomicBoolean[] CAS", "ConcurrentHashMap"};
    static final Maker[] MAKERS = {Bookers.SyncMethod::new, Bookers.SyncBlock::new, Bookers.LockBooker::new, Bookers.AtomicBooker::new, Bookers.MapBooker::new};

    /** threads x (attempts) bookings; returns elapsed ms and checks that sold == number of distinct seats. */
    static double run(Bookers.Booker b, int threads, int attempts, int seats) throws Exception {
        CyclicBarrier start = new CyclicBarrier(threads + 1);
        Thread[] ts = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int id = t;
            ts[t] = new Thread(() -> {
                try { start.await(); } catch (Exception e) { }
                for (int i = id; i < attempts; i += threads) b.book(i % seats);
            });
            ts[t].start();
        }
        long t0 = System.nanoTime();          // taken BEFORE releasing the threads (on one core they may finish before main resumes)
        start.await();
        for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / 1e6;
    }
    static long used() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    public static void main(String[] args) throws Exception {
        int threads = 8;
        System.out.println("processors = " + Runtime.getRuntime().availableProcessors() + ", client threads = " + threads);

        System.out.println();
        System.out.println("=== 1. 10,000 bookings over 1,000 seats (so every seat is requested 10 times). Time in ms, best of 5. ===");
        System.out.printf("%-26s %-22s %-26s %-8s%n", "strategy", "no external work", "100 us external work/booking", "seats sold (must be 1000)");
        for (int m = 0; m < MAKERS.length; m++) {
            double b0 = 1e18, b1 = 1e18; int sold0 = 0, sold1 = 0;
            for (int rep = 0; rep < 5; rep++) {
                Bookers.Booker x = MAKERS[m].make(1000, 0); b0 = Math.min(b0, run(x, threads, 10_000, 1000)); sold0 = x.sold();
                Bookers.Booker y = MAKERS[m].make(1000, 100); b1 = Math.min(b1, run(y, threads, 10_000, 1000)); sold1 = y.sold();
            }
            System.out.printf("%-26s %-22.1f %-26.1f %d / %d%n", NAMES[m], b0, b1, sold0, sold1);
        }
        System.out.println("(synchronized method holds the lock during the external work; every other strategy does it outside the lock)");

        System.out.println();
        System.out.println("=== 2. Time per booking under contention (ns, 400,000 attempts, 8 threads, best of 5) ===");
        System.out.printf("%-26s %-26s %-26s%n", "strategy", "HOT: 10 seats", "SPREAD: 100,000 seats");
        for (int m = 0; m < MAKERS.length; m++) {
            double hot = 1e18, spread = 1e18;
            for (int rep = 0; rep < 5; rep++) {
                hot = Math.min(hot, run(MAKERS[m].make(10, 0), threads, 400_000, 10) * 1e6 / 400_000);
                spread = Math.min(spread, run(MAKERS[m].make(100_000, 0), threads, 400_000, 100_000) * 1e6 / 400_000);
            }
            System.out.printf("%-26s %-26.0f %-26.0f%n", NAMES[m], hot, spread);
        }

        System.out.println();
        System.out.println("=== 3. Extra space for s = 200,000 seats (bytes per seat, measured) ===");
        int s = 200_000;
        long m0 = used(); boolean[] plain = new boolean[s * 50]; long m1 = used();   // 50x larger so that the measurement is not noise
        Bookers.AtomicBooker ab = new Bookers.AtomicBooker(s, 0); long m2 = used();
        Bookers.MapBooker mb = new Bookers.MapBooker(s, 0); for (int i = 0; i < s; i++) mb.book(i); long m3 = used();
        System.out.printf("boolean[] (any lock-based version) : %.1f bytes/seat  (+ one lock object, O(1))%n", (m1 - m0) / (double) (s * 50));
        System.out.printf("AtomicBoolean[]                    : %.1f bytes/seat  (one AtomicBoolean object per seat)%n", (m2 - m1) / (double) s);
        System.out.printf("ConcurrentHashMap (all seats sold) : %.1f bytes/seat  (Node + Integer key + table slot)%n", (m3 - m2) / (double) s);
        System.out.println("keep-alive " + (plain.length + ab.sold() + mb.sold() > 0));
    }
}
