import java.util.*;
import java.util.concurrent.*;

/** Student B: the same two transfers with lock ordering, then a stress test with random transfers. */
public class DeadlockFixed {
    interface Transfer { void run(Wallet f, Wallet t, long amt) throws Exception; }

    static String stress(String name, Transfer tr, int wallets, int threads, int perThread) throws Exception {
        Wallet[] w = new Wallet[wallets];
        for (int i = 0; i < wallets; i++) w[i] = new Wallet(i, 10_000);
        long before = 0; for (Wallet x : w) before += x.balance;
        Thread[] ts = new Thread[threads];
        long t0 = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                try { for (int k = 0; k < perThread; k++) { int f = r.nextInt(wallets), t = r.nextInt(wallets); if (f != t) tr.run(w[f], w[t], 1 + r.nextInt(5)); } } catch (Exception e) { }
            });
            ts[i].setDaemon(true); ts[i].start();
        }
        long deadline = System.nanoTime() + 5_000_000_000L; boolean hung = false;
        for (Thread t : ts) { t.join(Math.max(1, (deadline - System.nanoTime()) / 1_000_000)); if (t.isAlive()) hung = true; }
        double ms = (System.nanoTime() - t0) / 1e6;
        long after = 0; for (Wallet x : w) after += x.balance;
        return String.format("%-34s %s  money before=%d after=%d (%s)  time=%.0f ms", name, hung ? "HUNG (deadlock)" : "finished      ", before, after, before == after ? "conserved" : "NOT conserved", ms);
    }
    public static void main(String[] args) throws Exception {
        System.out.println("=== The two conflicting transfers with lock ordering (lower wallet id locked first) ===");
        Wallet a = new Wallet(1, 1000), b = new Wallet(2, 1000);
        Thread t1 = new Thread(() -> Wallet.transferOrdered(a, b, 100, 200));
        Thread t2 = new Thread(() -> Wallet.transferOrdered(b, a, 50, 200));
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.println("both transfers completed: A=" + a.balance + " B=" + b.balance + "   (expected A=950, B=1050)");

        System.out.println();
        System.out.println("=== Stress: 8 threads x 200 random transfers between 6 wallets, 1 ms pause between the two locks ===");
        System.out.println(stress("unsafe order (from, to)", (f, t, m) -> Wallet.transferUnsafe(f, t, m, 1), 6, 8, 200));
        System.out.println(stress("ordered locks (lower id first)", (f, t, m) -> Wallet.transferOrdered(f, t, m, 1), 6, 8, 200));
        System.out.println(stress("tryLock with timeout + back-off", (f, t, m) -> Wallet.transferTryLock(f, t, m, 1), 6, 8, 200));
    }
}
