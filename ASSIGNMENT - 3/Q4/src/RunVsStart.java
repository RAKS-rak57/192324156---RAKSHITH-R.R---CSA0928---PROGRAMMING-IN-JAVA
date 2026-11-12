/** Student A: run() is an ordinary method call; only start() creates a new thread. */
public class RunVsStart {
    public static void main(String[] args) throws Exception {
        Runnable r = () -> System.out.println("   body executed by thread: " + Thread.currentThread().getName() + "  (id " + Thread.currentThread().threadId() + ")");
        System.out.println("main thread id = " + Thread.currentThread().threadId());
        System.out.println("calling t.run():");
        Thread t1 = new Thread(r, "station-1");
        t1.run();                                     // runs in MAIN, no new thread
        System.out.println("   t1 state after run(): " + t1.getState() + "   (the thread was never started)");
        System.out.println("calling t.start():");
        Thread t2 = new Thread(r, "station-2");
        t2.start(); t2.join();                        // runs in station-2
        System.out.println("starting the same thread twice:");
        try { t2.start(); } catch (IllegalThreadStateException e) { System.out.println("   " + e); }

        // proof by timing: four 100 ms jobs
        Runnable job = () -> { try { Thread.sleep(100); } catch (InterruptedException e) { } };
        long t0 = System.nanoTime();
        for (int i = 0; i < 4; i++) new Thread(job).run();
        long viaRun = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) { ts[i] = new Thread(job); ts[i].start(); }
        for (Thread x : ts) x.join();
        long viaStart = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("four 100 ms jobs: via run() = " + viaRun + " ms (sequential, one after another), via start() = " + viaStart + " ms (overlapping)");
    }
}
