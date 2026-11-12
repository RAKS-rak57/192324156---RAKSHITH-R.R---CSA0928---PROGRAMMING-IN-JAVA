package results;

import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;

/** Student B: total time for 10,000 files with pools of 1, 2, 4, 8 (and 16, 32) threads. Writes raw data to a CSV file. */
public class ScalingExperiment {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        List<Path> files = Main.readIndex(dir);
        String expected = Files.readString(dir.resolve("expected_summary.txt")).trim();
        int[] pools = {1, 2, 4, 8, 16, 32};
        long[] latencies = {0, 500};
        System.out.println("processors = " + Runtime.getRuntime().availableProcessors() + ", files = " + files.size());
        for (int w = 0; w < 25; w++) ResultProcessor.process(files, 4, 0);            // global warm-up so the JIT has settled before ANY measurement
        try (PrintWriter csv = new PrintWriter("output/measurements.csv")) {
            csv.println("latency_us,threads,run,millis,summary_correct");
            for (long lat : latencies) {
                System.out.println();
                System.out.println(lat == 0 ? "=== A. real file reading only (no simulated latency) ===" : "=== B. with " + lat + " us simulated storage latency per file (network disk / slow share) ===");
                System.out.printf("%-8s %-34s %-10s %-12s %-10s %-8s%n", "threads", "all runs (ms)", "best", "median", "speed-up", "correct");
                double base = -1;
                for (int p : pools) {
                    int runs = lat == 0 ? 7 : 2;
                    double[] ms = new double[runs]; boolean allCorrect = true;
                    for (int w = 0; w < 4; w++) ResultProcessor.process(files, p, 0);      // warm-up (JIT, file cache) - not recorded
                    for (int r = 0; r < runs; r++) {
                        ResultProcessor.Run run = ResultProcessor.process(files, p, lat);
                        ms[r] = run.millis();
                        boolean ok = run.summary().counts().equals(expected) && run.summary().complete;
                        allCorrect &= ok;
                        csv.println(lat + "," + p + "," + (r + 1) + "," + String.format(Locale.ROOT, "%.1f", ms[r]) + "," + ok);
                    }
                    double[] sorted = ms.clone(); Arrays.sort(sorted);
                    double best = sorted[0], median = sorted[runs / 2];
                    if (base < 0) base = median;
                    StringBuilder all = new StringBuilder(); for (double m : ms) all.append(String.format("%.0f ", m));
                    System.out.printf("%-8d %-34s %-10.0f %-12.0f %-10.2f %-8s%n", p, all.toString().trim(), best, median, base / median, allCorrect);
                }
            }
        }
        System.out.println();
        System.out.println("raw data written to output/measurements.csv");
    }
}
