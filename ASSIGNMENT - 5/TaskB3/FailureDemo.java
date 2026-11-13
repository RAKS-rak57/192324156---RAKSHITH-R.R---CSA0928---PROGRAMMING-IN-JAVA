import java.io.File;
import java.sql.*;
import java.util.*;

/** Task B3.3: a failure in the middle of the load: how many rows stay in the table with and without a transaction? */
public class FailureDemo {
    interface Loader { void load(Connection c, List<String[]> l) throws Exception; }
    static int attempt(String title, List<String[]> data, Loader loader) throws Exception {
        File f = Db.newFile("fail");
        try (Connection c = Db.fresh(f, "bus_pass.sql")) {
            String error = "none";
            try { loader.load(c, data); } catch (Exception e) { error = e.getClass().getSimpleName() + ": " + e.getMessage().replaceAll("\\s+\\[.*", ""); }
            int rows = Db.count(c);
            System.out.printf("   %-52s -> %s%n      rows left in the table: %d   (auto-commit mode afterwards: %s)%n", title, error, rows, c.getAutoCommit());
            return rows;
        } finally { f.delete(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Failure 1: row 500 of 1,000 has a non-numeric amount (\"abc\") ===");
        List<String[]> d1 = PassData.generate(1000); d1.get(500)[4] = "abc";
        attempt("PassLoader: one transaction, batches of 1,000", d1, (c, l) -> PassLoader.loadAll(c, l));
        attempt("auto-commit, one PreparedStatement per row", d1, (c, l) -> PassInserters.prepared(c, l));
        attempt("auto-commit, batches of 100", d1, (c, l) -> PassInserters.batch(c, l, 100));

        System.out.println();
        System.out.println("=== Failure 2: row 500 of 1,000 repeats the pass id of row 10 (the DATABASE rejects it) ===");
        List<String[]> d2 = PassData.generate(1000); d2.get(500)[0] = d2.get(10)[0];
        attempt("PassLoader: one transaction, batches of 1,000", d2, (c, l) -> PassLoader.loadAll(c, l));
        attempt("auto-commit, one PreparedStatement per row", d2, (c, l) -> PassInserters.prepared(c, l));
        attempt("auto-commit, batches of 100", d2, (c, l) -> PassInserters.batch(c, l, 100));

        System.out.println();
        System.out.println("=== Control: no failure ===");
        attempt("PassLoader with 1,000 valid rows", PassData.generate(1000), (c, l) -> System.out.println("      loaded " + PassLoader.loadAll(c, l) + " rows"));
    }
}
