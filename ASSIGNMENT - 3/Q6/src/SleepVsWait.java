/** Student A: sleep() keeps the lock, wait() releases it. */
public class SleepVsWait {
    static final Object LOCK = new Object();
    static long measure(boolean useWait) throws Exception {
        long[] enteredAfter = new long[1];
        Thread holder = new Thread(() -> {
            synchronized (LOCK) {
                try { if (useWait) LOCK.wait(400); else Thread.sleep(400); } catch (InterruptedException e) { }
            }
        });
        holder.start();
        Thread.sleep(100);                                    // let the holder get inside first
        long t0 = System.nanoTime();
        Thread other = new Thread(() -> { synchronized (LOCK) { enteredAfter[0] = (System.nanoTime() - t0) / 1_000_000; } });
        other.start(); other.join(); holder.join();
        return enteredAfter[0];
    }
    public static void main(String[] args) throws Exception {
        System.out.println("holder thread is inside synchronized(LOCK) for 400 ms; another thread tries to enter 100 ms later:");
        System.out.println("  holder uses Thread.sleep(400): the other thread got in after " + measure(false) + " ms   (sleep KEEPS the lock)");
        System.out.println("  holder uses LOCK.wait(400)   : the other thread got in after " + measure(true) + " ms     (wait RELEASES the lock)");
        System.out.println();
        System.out.println("sleep(): static method of Thread, needs no lock, keeps any lock it holds, wakes up when the time is over.");
        System.out.println("wait() : method of Object, must be called inside synchronized, releases that lock, wakes on notify()/notifyAll()/timeout and must re-acquire the lock.");
    }
}
