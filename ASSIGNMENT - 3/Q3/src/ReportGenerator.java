import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * Daily sales report: read several store files, write one summary file.
 * Four implementations of the SAME job:
 *   leaky()   - close() at the end of the try block (the bug that produces "Too many open files")
 *   manual()  - try / catch / finally with manual close()
 *   twr()     - try-with-resources
 *   noClose() - never closes anything
 */
public class ReportGenerator {
    static final Logger LOG = Logger.getLogger("report");
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[LOG] %4$s: %5$s%n");
        LOG.setUseParentHandlers(true);
    }

    static class ReportException extends Exception {
        final String file;
        ReportException(String file, Throwable cause) { super("Report failed at " + file + ": " + cause.getMessage(), cause); this.file = file; }
    }

    /** Reads one store file, writes "name,revenue" to the summary and returns the revenue. */
    static double processStore(BufferedReader r, String name, BufferedWriter out) throws IOException {
        double revenue = 0; int lineNo = 0; String line;
        while ((line = r.readLine()) != null) {
            lineNo++;
            if (line.isBlank()) continue;
            String[] p = line.split(",");
            try { revenue += Integer.parseInt(p[1].trim()) * Double.parseDouble(p[2].trim()); }
            catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IOException("bad data in " + name + " line " + lineNo + ": \"" + line + "\"", e);
            }
        }
        out.write(name + "," + String.format(Locale.ROOT, "%.2f", revenue)); out.newLine();
        return revenue;
    }
    static void total(BufferedWriter out, double total) throws IOException {
        out.write("TOTAL," + String.format(Locale.ROOT, "%.2f", total)); out.newLine();
    }

    // ------------------------------------------------------------------ V0: leaky
    static void leaky(List<File> stores, File report) throws ReportException {
        BufferedWriter out = null; double total = 0;
        try {
            out = new Tracked.Writer(report);
            for (File f : stores) {
                try {
                    BufferedReader r = new Tracked.Reader(f);
                    total += processStore(r, f.getName(), out);
                    r.close();                                   // skipped when processStore throws
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "FAILED while reading " + f.getName() + ": " + e.getMessage());
                    throw new ReportException(f.getName(), e);
                }
            }
            total(out, total);
            out.close();                                         // skipped on failure
        } catch (IOException e) { throw new ReportException(report.getName(), e); }
    }

    // ------------------------------------------------------------------ V1: try / catch / finally
    static void closeQuietly(Closeable c, String what) {
        if (c == null) return;
        try { c.close(); } catch (IOException e) { LOG.warning("close() failed for " + what + ": " + e.getMessage()); }
    }
    static void manual(List<File> stores, File report) throws ReportException {
        BufferedWriter out = null; double total = 0;
        try {
            out = new Tracked.Writer(report);
            for (File f : stores) {
                BufferedReader r = null;
                try {
                    r = new Tracked.Reader(f);
                    total += processStore(r, f.getName(), out);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "FAILED while reading " + f.getName() + ": " + e.getMessage());
                    throw new ReportException(f.getName(), e);
                } finally {
                    closeQuietly(r, f.getName());                // always runs
                }
            }
            total(out, total);
        } catch (IOException e) {
            throw new ReportException(report.getName(), e);
        } finally {
            closeQuietly(out, report.getName());                 // flushes what was written so far
        }
    }

    // ------------------------------------------------------------------ V2: try-with-resources
    static void twr(List<File> stores, File report) throws ReportException {
        double total = 0;
        try (BufferedWriter out = new Tracked.Writer(report)) {
            for (File f : stores) {
                try (BufferedReader r = new Tracked.Reader(f)) {
                    total += processStore(r, f.getName(), out);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "FAILED while reading " + f.getName() + ": " + e.getMessage());
                    throw new ReportException(f.getName(), e);
                }
            }
            total(out, total);
        } catch (IOException e) {
            throw new ReportException(report.getName(), e);
        }
    }

    // ------------------------------------------------------------------ V3: no closing at all
    static void noClose(List<File> stores, File report) throws ReportException {
        double total = 0; String current = report.getName();
        try {
            BufferedWriter out = new Tracked.Writer(report);
            for (File f : stores) {
                current = f.getName();
                BufferedReader r = new Tracked.Reader(f);
                total += processStore(r, f.getName(), out);
            }
            current = report.getName();
            total(out, total);
            out.flush();
        } catch (IOException e) { LOG.log(Level.SEVERE, "FAILED while reading " + current + ": " + e.getMessage()); throw new ReportException(current, e); }
    }

    static List<File> listStores(String dir) {
        File[] fs = new File(dir).listFiles((d, n) -> n.endsWith(".csv"));
        Arrays.sort(fs);
        return Arrays.asList(fs);
    }
}
