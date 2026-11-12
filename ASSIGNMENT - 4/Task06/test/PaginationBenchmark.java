import campus.reg.dao.InMemoryRegistrationDao;
import campus.reg.model.RegistrationRow;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Alternative concept: load ALL 100,000 registrations into the JTable model at once  versus  one page of 50 rows.
 * The "database" is an in-memory DAO (no network), so the fetch times are lower than MySQL would give; the TABLE-MODEL work
 * (which must run on the event-dispatch thread) and the memory are real measurements.
 */
public class PaginationBenchmark {
    static final String[] COLS = {"Reg ID", "Register No", "Student", "Event", "Registered on"};
    static long sink = 0;

    static long allocated() {
        com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        return b.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    static DefaultTableModel fill(List<RegistrationRow> rows) {
        DefaultTableModel m = new DefaultTableModel(COLS, 0);
        for (RegistrationRow r : rows) m.addRow(new Object[]{r.regId(), r.regNo(), r.studentName(), r.eventTitle(), r.registeredOn()});
        return m;
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> { });                 // start the event-dispatch thread before measuring
        PrintWriter csv = new PrintWriter("pagination_results.csv");
        csv.println("rows,approach,fetch_ms,edt_model_fill_ms,edt_allocated_MB");
        System.out.println("=== Loading registrations into a JTable model: everything at once vs one page of 50 (average of 5 runs) ===");
        System.out.printf("%9s %-14s %12s %24s %22s%n", "rows", "approach", "fetch (ms)", "EDT blocked: fill (ms)", "EDT allocation (MB)");
        for (int n : new int[]{1000, 10000, 100000}) {
            InMemoryRegistrationDao dao = new InMemoryRegistrationDao(0);
            dao.bulkAdd(n);
            for (int w = 0; w < 5; w++) { timedFill(dao.page(null, 50, 0)); timedFill(dao.findAll(null)); }      // warm-up
            for (int mode = 0; mode < 2; mode++) {
                double fetch = 0, fillMs = 0, mb = 0;
                for (int r = 0; r < 5; r++) {
                    long t = System.nanoTime();
                    List<RegistrationRow> rows = (mode == 0) ? dao.findAll(null)                 // SQL without LIMIT
                                                             : dao.page(null, 50, n / 2);        // SQL ... LIMIT 50 OFFSET n/2
                    fetch += (System.nanoTime() - t) / 1e6;
                    double[] f = timedFill(rows);
                    fillMs += f[0]; mb += f[1];
                }
                String name = mode == 0 ? "all at once" : "page of 50";
                System.out.printf("%9d %-14s %12.2f %24.2f %22.2f%n", n, name, fetch / 5, fillMs / 5, mb / 5);
                csv.printf("%d,%s,%.3f,%.3f,%.3f%n", n, name, fetch / 5, fillMs / 5, mb / 5);
            }
        }
        csv.close();
        System.out.println("(checksum " + sink + ")");
    }

    /** Fills a table model ON the event-dispatch thread and returns {milliseconds the EDT was busy, MB allocated by the EDT}. */
    static double[] timedFill(List<RegistrationRow> rows) throws Exception {
        final double[] out = new double[2];
        SwingUtilities.invokeAndWait(() -> {
            long a0 = allocated(), t = System.nanoTime();
            DefaultTableModel m = fill(rows);
            out[0] = (System.nanoTime() - t) / 1e6;
            out[1] = (allocated() - a0) / 1048576.0;
            sink += m.getRowCount();
        });
        return out;
    }
}
