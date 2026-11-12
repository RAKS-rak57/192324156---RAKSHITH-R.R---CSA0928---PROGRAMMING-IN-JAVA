package results;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Alternative designs for the same job.
 *   java results.Alternatives parstream <dir> <latencyUs>        files.parallelStream(), failures handled per element
 *   java results.Alternatives parstream-naive <dir> <latencyUs>  files.parallelStream(), NO per-element handling
 *   java results.Alternatives thread-per-file <dir> <latencyUs>  one new Thread per file
 */
public class Alternatives {
    static long status(String key) {
        try { for (String l : Files.readAllLines(Paths.get("/proc/self/status"))) if (l.startsWith(key)) return Long.parseLong(l.replaceAll("[^0-9]", "")); } catch (Exception e) { }
        return -1;
    }
    public static void main(String[] args) throws Exception {
        String mode = args[0]; Path dir = Paths.get(args[1]); long lat = Long.parseLong(args[2]);
        List<Path> files = Main.readIndex(dir);
        String expected = Files.readString(dir.resolve("expected_summary.txt")).trim();
        System.out.println("mode = " + mode + ", files = " + files.size() + ", simulated latency = " + lat + " us, processors = " + Runtime.getRuntime().availableProcessors()
                + ", common pool parallelism = " + ForkJoinPool.commonPool().getParallelism());
        ConcurrentMap<String, FileResult> sink = new ConcurrentHashMap<>();
        long rss0 = status("VmRSS"), vm0 = status("VmSize");
        long t0 = System.nanoTime();

        if (mode.equals("parstream")) {
            files.parallelStream().forEach(f -> new ScoreFileTask(f, sink, lat).call());          // the task converts failures into results
            report(sink, files.size(), expected, t0, rss0, vm0);

        } else if (mode.equals("parstream-naive")) {
            AtomicInteger done = new AtomicInteger();
            try {
                files.parallelStream().forEach(f -> {
                    try {
                        if (lat > 0) Thread.sleep(lat / 1000, (int) (lat % 1000) * 1000);
                        ScoreParser.parse(Files.readString(f));                                    // a missing / corrupt file THROWS out of the lambda
                        done.incrementAndGet();
                    } catch (Exception e) { throw new RuntimeException(f.getFileName() + ": " + e, e); }
                });
                System.out.println("finished without exception (unexpected)");
            } catch (RuntimeException e) {
                System.out.printf("whole run ABORTED after %d of %d files were processed: %s%n", done.get(), files.size(), e.getMessage());
                System.out.println("no summary can be produced; the other files' results are lost (not collected anywhere).");
            }
            System.out.printf("time until abort = %.0f ms%n", (System.nanoTime() - t0) / 1e6);

        } else if (mode.equals("thread-per-file")) {
            List<Thread> ts = new ArrayList<>();
            try {
                for (Path f : files) { Thread t = new Thread(new FutureTask<>(new ScoreFileTask(f, sink, lat))); t.start(); ts.add(t); }
            } catch (Throwable e) {
                System.out.println("THREAD CREATION FAILED after " + ts.size() + " threads: " + e);
            }
            long rssPeak = status("VmRSS"), vmPeak = status("VmSize");
            for (Thread t : ts) t.join();
            System.out.println("peak live threads = " + ManagementFactory.getThreadMXBean().getPeakThreadCount());
            System.out.printf("memory while the threads were alive: resident +%d KB, reserved virtual +%d KB%n", rssPeak - rss0, vmPeak - vm0);
            report(sink, files.size(), expected, t0, rss0, vm0);
        }
    }
    static void report(ConcurrentMap<String, FileResult> sink, int n, String expected, long t0, long rss0, long vm0) {
        double ms = (System.nanoTime() - t0) / 1e6;
        Summary s = new Summary(n, sink.values());
        System.out.printf("time = %.0f ms   %s   complete=%s   %s%n", ms, s.counts(), s.complete, s.counts().equals(expected) ? "MATCH" : "MISMATCH");
    }
}
