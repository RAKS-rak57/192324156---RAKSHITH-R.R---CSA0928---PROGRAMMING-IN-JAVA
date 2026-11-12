import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Student A: while vs if, and notify vs notifyAll, under stress. Each trial has a 3 second watchdog. */
public class CounterStress {
    /** returns "OK", "WRONG SUM x", "HUNG" or "NEGATIVE/overflow size" */
    static String trial(int capacity, int producers, int consumers, int perProducer, boolean all, boolean useWhile) throws Exception {
        BoundedCounter c = new BoundedCounter(capacity, all, useWhile);
        int total = producers * perProducer;
        long expected = (long) producers * (perProducer * (perProducer + 1L) / 2);
        AtomicLong sum = new AtomicLong();
        Thread[] ts = new Thread[producers + consumers];
        for (int p = 0; p < producers; p++) ts[p] = new Thread(() -> { try { for (int t = 1; t <= perProducer; t++) c.put(t); } catch (InterruptedException e) { } });
        for (int k = 0; k < consumers; k++) {
            int share = total / consumers;
            ts[producers + k] = new Thread(() -> { try { for (int i = 0; i < share; i++) sum.addAndGet(c.take()); } catch (InterruptedException e) { } });
        }
        for (Thread t : ts) { t.setDaemon(true); t.start(); }
        long deadline = System.nanoTime() + 2_000_000_000L;
        for (Thread t : ts) { long left = Math.max(1, (deadline - System.nanoTime()) / 1_000_000); t.join(left); }
        boolean hung = false; for (Thread t : ts) if (t.isAlive()) hung = true;
        for (Thread t : ts) if (t.isAlive()) t.interrupt();
        if (hung) return "HUNG";
        if (c.minSeen < 0 || c.maxSeen > capacity) return "SIZE OUT OF RANGE (min " + c.minSeen + ", max " + c.maxSeen + ")";
        return sum.get() == expected ? "OK" : "WRONG SUM " + sum.get() + " (expected " + expected + ")";
    }
    static void series(String title, int capacity, int p, int k, int per, boolean all, boolean useWhile, int trials) throws Exception {
        java.util.Map<String, Integer> count = new java.util.TreeMap<>();
        for (int i = 0; i < trials; i++) count.merge(trial(capacity, p, k, per, all, useWhile).replaceAll("\\(.*\\)", "").replaceAll("[0-9]+", "n").trim(), 1, Integer::sum);
        System.out.printf("%-62s -> %s%n", title, count);
    }
    public static void main(String[] args) throws Exception {
        int T = 10;
        System.out.println("(" + T + " trials each; HUNG = threads still blocked after 2 seconds)");
        System.out.println("--- Part 1: while vs if  (notifyAll in both) ---");
        series("while, capacity 5, 1 producer, 2 consumers (the assignment)", 5, 1, 2, 10, true, true, T);
        series("if,    capacity 5, 1 producer, 2 consumers (the assignment)", 5, 1, 2, 10, true, false, T);
        series("while, capacity 1, 2 producers, 4 consumers, 500 tokens each", 1, 2, 4, 500, true, true, T);
        series("if,    capacity 1, 2 producers, 4 consumers, 500 tokens each", 1, 2, 4, 500, true, false, T);
        System.out.println("--- Part 2: notify() vs notifyAll()  (while in both) ---");
        series("notifyAll, 1 producer, 2 consumers, capacity 5", 5, 1, 2, 10, true, true, T);
        series("notify,    1 producer, 2 consumers, capacity 5", 5, 1, 2, 10, false, true, T);
        series("notifyAll, 2 producers, 4 consumers, capacity 1", 1, 2, 4, 500, true, true, T);
        series("notify,    2 producers, 4 consumers, capacity 1", 1, 2, 4, 500, false, true, T);
    }
}
