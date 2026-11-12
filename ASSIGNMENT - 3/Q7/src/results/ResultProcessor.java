package results;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Design: ExecutorService + Callable + Future + ConcurrentHashMap. One bad file can never stop the run. */
public class ResultProcessor {
    public record Run(Summary summary, double millis) { }

    /** Creates the Callable for one file (a test can supply a faulty one). */
    public interface TaskFactory { Callable<FileResult> create(Path file, ConcurrentMap<String, FileResult> sink, long latencyMicros); }

    public static Run process(List<Path> files, int threads, long latencyMicros) throws InterruptedException {
        return process(files, threads, latencyMicros, ScoreFileTask::new);
    }

    public static Run process(List<Path> files, int threads, long latencyMicros, TaskFactory factory) throws InterruptedException {
        ConcurrentMap<String, FileResult> results = new ConcurrentHashMap<>(Math.max(16, files.size() * 2));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long t0 = System.nanoTime();
        List<Map.Entry<Path, Future<FileResult>>> futures = new ArrayList<>(files.size());
        try {
            for (Path f : files) futures.add(Map.entry(f, pool.submit(factory.create(f, results, latencyMicros))));
            for (Map.Entry<Path, Future<FileResult>> e : futures) {
                String name = e.getKey().getFileName().toString();
                try {
                    e.getValue().get();                                     // wait for this file's task
                } catch (ExecutionException ex) {                           // the task itself threw: still record the file
                    results.putIfAbsent(name, FileResult.failed(name, Status.ERROR, "task failed: " + ex.getCause()));
                } catch (CancellationException ex) {
                    results.putIfAbsent(name, FileResult.failed(name, Status.ERROR, "cancelled"));
                }
            }
        } finally {
            pool.shutdown();                                                // always release the worker threads
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) pool.shutdownNow();
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        return new Run(new Summary(files.size(), results.values()), ms);
    }
}
