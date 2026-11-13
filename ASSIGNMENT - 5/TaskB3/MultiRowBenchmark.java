import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.*;
import java.util.List;

/** Alternative of B3: ONE INSERT statement with b rows, compared with addBatch()/executeBatch() (both in one transaction). */
public class MultiRowBenchmark {
    static long allocated() { com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean(); return b.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    public static void main(String[] args) throws Exception {
        Db.dir = args.length > 0 ? args[0] : "/tmp";
        System.out.println("=== Multi-row INSERT against addBatch (one transaction each, database in " + Db.dir + ") ===");
        System.out.printf("%9s %-30s %12s %14s %18s%n", "rows", "approach", "time (ms)", "us per row", "client bytes / row");
        for (int w = 0; w < 2; w++) for (int a = 0; a < 3; a++) one(2000, a, false);                      // warm-up
        for (int n : new int[]{10000, 100000}) {
            for (int a = 0; a < 4; a++) one(n, a, true);
        }
        int b = 500;
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES " + String.join(", ", java.util.Collections.nCopies(b, "(?, ?, ?, ?, ?)"));
        System.out.println();
        System.out.println("SQL text of ONE multi-row statement with b = 500 rows: " + sql.length() + " characters (" + sql.length() * 2 + " bytes as a Java String), 2,500 parameters");
        System.out.println("LOAD DATA (MySQL only, not available in this test engine):  LOAD DATA LOCAL INFILE 'passes.csv' INTO TABLE bus_pass FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';");
    }
    static String[] NAMES = {"addBatch, batches of 1,000", "multi-row INSERT, b = 100", "multi-row INSERT, b = 500", "PreparedStatement per row"};
    static void one(int n, int approach, boolean print) throws Exception {
        List<String[]> passes = PassData.generate(n);
        File f = Db.newFile("multi");
        try (Connection c = Db.fresh(f, "bus_pass.sql")) {
            c.setAutoCommit(false);
            long a0 = allocated(), t = System.nanoTime();
            switch (approach) {
                case 0: PassInserters.batch(c, passes, 1000); break;
                case 1: PassInserters.multiRow(c, passes, 100); break;
                case 2: PassInserters.multiRow(c, passes, 500); break;
                default: PassInserters.prepared(c, passes);
            }
            c.commit();
            double ms = (System.nanoTime() - t) / 1e6; long bytes = allocated() - a0;
            if (Db.count(c) != n) throw new IllegalStateException("wrong count");
            if (print) System.out.printf("%,9d %-30s %12.1f %14.2f %,18d%n", n, NAMES[approach], ms, ms * 1000 / n, bytes / n);
        } finally { f.delete(); }
    }
}
