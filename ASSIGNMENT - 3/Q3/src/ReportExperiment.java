import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Student A: same input, four versions - what is left open, and what does the report contain? */
public class ReportExperiment {
    interface Version { void run(List<File> s, File r) throws ReportGenerator.ReportException; }

    static String sha(File f) throws Exception {
        if (!f.exists()) return "(no file)";
        byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
        StringBuilder sb = new StringBuilder(); for (int i = 0; i < 4; i++) sb.append(String.format("%02x", d[i])); return sb.toString();
    }
    public static void main(String[] args) throws Exception {
        String[] names = {"leaky (close in try)", "manual try/catch/finally", "try-with-resources", "never closed"};
        Version[] vs = {ReportGenerator::leaky, ReportGenerator::manual, ReportGenerator::twr, ReportGenerator::noClose};
        String[] tags = {"leaky", "manual", "twr", "noclose"};

        System.out.println("=== 1. SAME GOOD INPUT (input/stores: 5 files) - do all versions produce the same report? ===");
        System.out.printf("%-26s %-12s %-10s %-14s %-14s%n", "version", "report hash", "lines", "tracked open", "OS fds open");
        Map<String, String> hashes = new LinkedHashMap<>();
        for (int i = 0; i < vs.length; i++) {
            Tracked.reset();
            File rep = new File("/tmp/report_" + tags[i] + "_good.csv"); rep.delete();
            vs[i].run(ReportGenerator.listStores("input/stores"), rep);
            int tracked = Tracked.openCount(), fds = Tracked.osOpenFds("stores", "report_");
            String h = sha(rep); int lines = rep.exists() ? Files.readAllLines(rep.toPath()).size() : 0;   // measured BEFORE cleanup
            Tracked.closeAllLeaked();
            hashes.put(tags[i], h);
            System.out.printf("%-26s %-12s %-10d %-14d %-14d%n", names[i], h, lines, tracked, fds);
        }
        System.out.println("manual == try-with-resources report: " + hashes.get("manual").equals(hashes.get("twr")) + "   (leaky and never-closed also equal on SUCCESS: " + (hashes.get("leaky").equals(hashes.get("twr")) && hashes.get("noclose").equals(hashes.get("twr"))) + ")");
        System.out.println("-> on a successful run the leaky version only looks fine because it closes at the end; the never-closed version leaks even on success.");

        System.out.println();
        String[] snapshot = new String[4];
        System.out.println("=== 2. FAILING INPUT (input/stores_bad: store3.csv has a bad number on line 3) ===");
        System.out.printf("%-26s %-12s %-10s %-14s %-14s %s%n", "version", "report hash", "lines", "tracked open", "OS fds open", "exception");
        for (int i = 0; i < vs.length; i++) {
            Tracked.reset();
            File rep = new File("/tmp/report_" + tags[i] + "_bad.csv"); rep.delete();
            String ex = "none";
            try { vs[i].run(ReportGenerator.listStores("input/stores_bad"), rep); }
            catch (ReportGenerator.ReportException e) { ex = e.file; }
            int tracked = Tracked.openCount(), fds = Tracked.osOpenFds("stores", "report_");
            List<String> open = new ArrayList<>(); for (Closeable c : Tracked.OPEN) open.add(c instanceof Tracked.Writer ? "summary-writer" : "reader");
            String h = sha(rep); int lines = rep.exists() ? Files.readAllLines(rep.toPath()).size() : 0;   // measured BEFORE cleanup
            snapshot[i] = rep.exists() ? new String(Files.readAllBytes(rep.toPath())) : "";
            Tracked.closeAllLeaked();
            System.out.printf("%-26s %-12s %-10d %-14d %-14d failed at %s  leaked=%s%n", names[i], h, lines, tracked, fds, ex, open);
        }
        System.out.println();
        System.out.println("=== report file content at the moment of failure (before any clean-up) ===");
        for (int i = 0; i < vs.length; i++) {
            System.out.println("--- " + names[i] + " (" + snapshot[i].length() + " bytes on disk when the program failed) ---");
            System.out.print(snapshot[i]);
        }
    }
}
