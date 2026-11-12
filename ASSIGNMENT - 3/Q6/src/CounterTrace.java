/** Student A: record the counter size after every put/take to show it stays within 0..5. */
public class CounterTrace {
    public static void main(String[] args) throws Exception {
        BoundedCounter c = new BoundedCounter(5, true, true);
        c.record = true;
        long[] sum = new long[2];
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 20; t++) c.put(t); } catch (InterruptedException e) { } });
        Thread[] st = new Thread[2];
        for (int s = 0; s < 2; s++) { final int id = s; st[s] = new Thread(() -> { try { for (int k = 0; k < 10; k++) { sum[id] += c.take(); Thread.sleep(1); } } catch (InterruptedException e) { } }); }
        cook.start(); for (Thread t : st) t.start();
        cook.join(); for (Thread t : st) t.join();
        System.out.println("counter size after each of the 40 operations (put or take):");
        System.out.println(c.trace);
        System.out.println("minimum size seen = " + c.minSeen + ", maximum size seen = " + c.maxSeen + "   (limits: 0 and 5)");
        System.out.println("sum of tokens = " + (sum[0] + sum[1]) + " (expected 210), final size = " + c.size());
    }
}
