import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.List;

/**
 * Tasks B3.2 / B3.3: time to insert n passes with Statement, PreparedStatement and PreparedStatement batches,
 * each with auto-commit ON (every statement is committed separately) and inside ONE transaction.
 * usage: LoaderBenchmark <db directory> <sizes comma separated> <auto|tx|both> <output csv>
 */
public class LoaderBenchmark {
    static final String[] APPROACH = {"Statement", "PreparedStatement", "Prepared+Batch(1000)"};

    static double run(String dir, int n, int approach, boolean transaction) throws Exception {
        Db.dir = dir;
        List<String[]> passes = PassData.generate(n);
        File f = Db.newFile("bench");
        try (Connection c = Db.fresh(f, "bus_pass.sql")) {
            long t = System.nanoTime();
            if (transaction) {
                if (approach == 2) PassLoader.loadAll(c, passes);                 // the completed template: one transaction, batches of 1,000
                else {
                    c.setAutoCommit(false);
                    if (approach == 0) PassInserters.statement(c, passes); else PassInserters.prepared(c, passes);
                    c.commit();
                }
            } else {
                c.setAutoCommit(true);
                if (approach == 0) PassInserters.statement(c, passes); else if (approach == 1) PassInserters.prepared(c, passes); else PassInserters.batch(c, passes, 1000);
            }
            double ms = (System.nanoTime() - t) / 1e6;
            if (Db.count(c) != n) throw new IllegalStateException("row count wrong: " + Db.count(c) + " instead of " + n);
            return ms;
        } finally { f.delete(); }
    }

    public static void main(String[] args) throws Exception {
        String dir = args[0]; String mode = args[2];
        PrintWriter csv = new PrintWriter(new FileWriter(args[3], true), true);
        System.out.println("=== Insert n passes (database file in " + dir + "), time in milliseconds ===");
        System.out.printf("%9s %-22s %-14s %14s %14s%n", "rows", "approach", "commit mode", "total (ms)", "us per row");
        run(dir, 500, 0, true); run(dir, 500, 1, true); run(dir, 500, 2, true);            // warm-up
        for (String sz : args[1].split(",")) {
            int n = Integer.parseInt(sz.trim());
            for (int a = 0; a < 3; a++) {
                if (args.length > 4 && Integer.parseInt(args[4]) != a) continue;      // optional: run only one approach
                for (boolean tx : new boolean[]{false, true}) {
                    if (mode.equals("auto") && tx) continue;
                    if (mode.equals("tx") && !tx) continue;
                    int reps = (n <= 1000 && tx) ? 3 : 1;
                    double best = 1e18;
                    for (int r = 0; r < reps; r++) best = Math.min(best, run(dir, n, a, tx));
                    System.out.printf("%,9d %-22s %-14s %14.1f %14.2f%n", n, APPROACH[a], tx ? "one transaction" : "auto-commit", best, best * 1000 / n);
                    csv.printf("%s,%d,%s,%s,%.2f%n", dir, n, APPROACH[a], tx ? "transaction" : "autocommit", best);
                }
            }
        }
        csv.close();
    }
}
