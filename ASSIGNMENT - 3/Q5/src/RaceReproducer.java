import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Student A: a test that reliably reproduces double booking. 8 threads are released together to book the SAME seat. */
public class RaceReproducer {
    static int trial(Bookers.Booker b, int threads) throws Exception {
        CyclicBarrier gate = new CyclicBarrier(threads);
        AtomicInteger granted = new AtomicInteger();
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> { try { gate.await(); } catch (Exception e) { } if (b.book(0)) granted.incrementAndGet(); });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        return granted.get();          // how many customers were told "seat 0 is yours"
    }
    public static void main(String[] args) throws Exception {
        int trials = 500, threads = 8;
        System.out.println("Each trial: 8 customers press 'book' for the SAME seat at the same moment. A trial with granted > 1 is a DOUBLE BOOKING.");
        System.out.printf("%-34s %-8s %-8s %-8s %-8s %-8s | %-10s %-12s%n", "strategy", "batch1", "batch2", "batch3", "batch4", "batch5", "total/2500", "max granted");
        Object[][] cfg = {
            {"unsynchronized (natural race)", 0}, {"unsynchronized (window widened)", 1},
            {"synchronized method", 2}, {"synchronized block", 3}};
        for (Object[] c : cfg) {
            int total = 0, max = 0; StringBuilder row = new StringBuilder();
            for (int batch = 0; batch < 5; batch++) {
                int doubles = 0;
                for (int t = 0; t < trials; t++) {
                    Bookers.Booker b;
                    switch ((int) c[1]) {
                        case 0: b = new Bookers.Unsync(1, false); break;
                        case 1: b = new Bookers.Unsync(1, true); break;
                        case 2: b = new Bookers.SyncMethod(1, 0); break;
                        default: b = new Bookers.SyncBlock(1, 0); break;
                    }
                    int g = trial(b, threads);
                    if (g > 1) doubles++;
                    max = Math.max(max, g);
                }
                total += doubles; row.append(String.format("%-8d ", doubles));
            }
            System.out.printf("%-34s %s| %-10d %-12d%n", c[0], row, total, max);
        }
        System.out.println("(numbers are double bookings per 500 trials)");
    }
}
