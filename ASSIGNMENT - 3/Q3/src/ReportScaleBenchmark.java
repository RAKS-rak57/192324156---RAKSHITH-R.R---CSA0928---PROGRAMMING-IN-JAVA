import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Q3 alternative analysis: time, and the handles held open, as the number of store files f grows. */
public class ReportScaleBenchmark {
    interface Version { void run(List<File> s, File r) throws ReportGenerator.ReportException; }

    static File makeStores(int f, int linesPerFile, int badIndex) throws IOException {
        File dir = Files.createTempDirectory("stores_" + f + "_").toFile();
        for (int i = 0; i < f; i++) {
            try (PrintWriter w = new PrintWriter(new File(dir, String.format("store%05d.csv", i)))) {
                for (int k = 0; k < linesPerFile; k++) w.println("item" + k + "," + (k % 7 + 1) + "," + (k % 50 + 1) + ".25");
                if (i == badIndex) w.println("broken,line");
            }
        }
        return dir;
    }
    static void delete(File dir) { for (File x : dir.listFiles()) x.delete(); dir.delete(); }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("ulimit")) {           // run under a low "ulimit -n"
            java.util.logging.Logger.getLogger("report").setLevel(java.util.logging.Level.OFF);
            File dir = makeStores(400, 3, -1);
            try { ReportGenerator.noClose(ReportGenerator.listStores(dir.getPath()), new File("/tmp/ul_report.csv")); System.out.println("finished without error"); }
            catch (ReportGenerator.ReportException e) { System.out.println("FAILED after " + Tracked.openCount() + " handles were open: " + e.getCause().getMessage()); }
            Tracked.closeAllLeaked(); delete(dir); return;
        }
        String[] names = {"leaky", "manual finally", "try-with-resources", "never closed"};
        Version[] vs = {ReportGenerator::leaky, ReportGenerator::manual, ReportGenerator::twr, ReportGenerator::noClose};
        java.util.logging.Logger.getLogger("report").setLevel(java.util.logging.Level.OFF);

        System.out.println("=== Success run: time (ms, best of 5) and peak simultaneously open handles; f files x 200 lines ===");
        System.out.printf("%-8s %-10s %-10s %-20s %-14s %-24s%n", "f", "leaky", "manual", "try-with-resources", "never closed", "peak open (manual/twr | never closed)");
        for (int f : new int[]{10, 100, 1000}) {
            File dir = makeStores(f, 200, -1);
            List<File> stores = ReportGenerator.listStores(dir.getPath());
            double[] best = new double[4]; int[] peak = new int[4];
            for (int v = 0; v < 4; v++) {
                best[v] = 1e18;
                for (int rep = 0; rep < 5; rep++) {
                    Tracked.reset();
                    long t0 = System.nanoTime();
                    vs[v].run(stores, new File("/tmp/scale_report.csv"));
                    best[v] = Math.min(best[v], (System.nanoTime() - t0) / 1e6);
                    peak[v] = Tracked.peak; Tracked.closeAllLeaked();
                }
            }
            System.out.printf("%-8d %-10.1f %-10.1f %-20.1f %-14.1f %d / %d | %d%n", f, best[0], best[1], best[2], best[3], peak[1], peak[2], peak[3]);
            delete(dir);
        }

        System.out.println();
        System.out.println("=== Failure in the MIDDLE store (store f/2 is broken): handles still open after the failure ===");
        System.out.printf("%-8s %-10s %-16s %-22s %-14s%n", "f", "leaky", "manual finally", "try-with-resources", "never closed");
        for (int f : new int[]{10, 100, 1000}) {
            File dir = makeStores(f, 200, f / 2);
            List<File> stores = ReportGenerator.listStores(dir.getPath());
            int[] leaked = new int[4];
            for (int v = 0; v < 4; v++) {
                Tracked.reset();
                try { vs[v].run(stores, new File("/tmp/scale_report.csv")); } catch (ReportGenerator.ReportException e) { }
                leaked[v] = Tracked.openCount(); Tracked.closeAllLeaked();
            }
            System.out.printf("%-8d %-10d %-16d %-22d %-14d%n", f, leaked[0], leaked[1], leaked[2], leaked[3]);
            delete(dir);
        }
    }
}
