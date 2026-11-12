import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Q4 alternative: the stations run on a fixed thread pool (ExecutorService) instead of hand-made Thread objects. */
public class ExecutorStations {
    static int sleepMs = 5;
    static void script() { try { Thread.sleep(sleepMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    static long status(String key) throws Exception {
        for (String l : Files.readAllLines(Paths.get("/proc/self/status"))) if (l.startsWith(key)) return Long.parseLong(l.replaceAll("[^0-9]", ""));
        return -1;
    }
    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. The four stations on a fixed pool (4 batches of 20 scripts, Callable returns the count) ===");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Integer>> fs = new ArrayList<>();
        for (int s = 1; s <= 4; s++) { final int id = s; fs.add(pool.submit(() -> Scanner1.scan("S" + id, 20))); }
        int total = 0; for (Future<Integer> f : fs) total += f.get();               // get() waits, like join()
        pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("total scripts scanned = " + total + "   pool terminated = " + pool.isTerminated());

        System.out.println();
        System.out.println("=== 2. n = 400 scripts, each 5 ms of waiting, p pool threads: completion time vs the n/p batches model ===");
        System.out.printf("%-4s %-18s %-18s %-12s%n", "p", "model n*5ms/p (ms)", "measured (ms)", "efficiency");
        int n = 400;
        for (int p : new int[]{1, 2, 4, 8, 16, 50}) {
            double best = 1e18;
            for (int rep = 0; rep < 3; rep++) {
                ExecutorService ex = Executors.newFixedThreadPool(p);
                long t0 = System.nanoTime();
                List<Future<?>> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(ex.submit(ExecutorStations::script));
                for (Future<?> f : l) f.get();
                best = Math.min(best, (System.nanoTime() - t0) / 1e6);
                ex.shutdown();
            }
            double model = Math.ceil(n / (double) p) * sleepMs;
            System.out.printf("%-4d %-18.0f %-18.0f %-12.2f%n", p, model, best, model / best);
        }

        System.out.println();
        System.out.println("=== 3. Cost of creating threads yourself vs re-using pool threads: 2,000 tiny tasks ===");
        int tasks = 2000; double bt = 1e18, bp = 1e18;
        for (int rep = 0; rep < 5; rep++) {
            long t0 = System.nanoTime();
            Thread[] ts = new Thread[tasks];
            for (int i = 0; i < tasks; i++) { ts[i] = new Thread(() -> { }); ts[i].start(); }
            for (Thread t : ts) t.join();
            bt = Math.min(bt, (System.nanoTime() - t0) / 1e6);
            ExecutorService ex = Executors.newFixedThreadPool(4);
            long t1 = System.nanoTime();
            List<Future<?>> l = new ArrayList<>();
            for (int i = 0; i < tasks; i++) l.add(ex.submit(() -> { }));
            for (Future<?> f : l) f.get();
            bp = Math.min(bp, (System.nanoTime() - t1) / 1e6);
            ex.shutdown();
        }
        System.out.printf("new Thread per task: %.1f ms (%.1f us per task)   pool of 4: %.1f ms (%.1f us per task)%n", bt, bt * 1000 / tasks, bp, bp * 1000 / tasks);

        System.out.println();
        System.out.println("=== 4. Memory per thread (default stack size = 1024 KB, -Xss) ===");
        long rss0 = status("VmRSS"), vm0 = status("VmSize");
        int k = 500; CountDownLatch stop = new CountDownLatch(1); Thread[] ts = new Thread[k];
        for (int i = 0; i < k; i++) { ts[i] = new Thread(() -> { try { stop.await(); } catch (InterruptedException e) { } }); ts[i].start(); }
        Thread.sleep(300);
        long rss1 = status("VmRSS"), vm1 = status("VmSize");
        stop.countDown(); for (Thread t : ts) t.join();
        System.out.printf("%d parked threads: resident memory +%d KB (%.0f KB per thread), reserved virtual memory +%d KB (%.0f KB per thread)%n",
                k, rss1 - rss0, (rss1 - rss0) / (double) k, vm1 - vm0, (vm1 - vm0) / (double) k);
        System.out.println("pool of 4 threads: about 4 x that, whatever the number of tasks.");
    }
}
