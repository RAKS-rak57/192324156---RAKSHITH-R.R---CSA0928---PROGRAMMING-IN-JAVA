import java.io.File;
import java.nio.file.*;
import java.sql.*;

/** Task A5: transfer money between two accounts: ONE transaction against two independent auto-committed statements. */
public class TransferDemo {
    static final String FROM = "HOSTEL-192311001", TO = "CANTEEN-192311001";

    static Connection fresh(File f) throws Exception {
        minisqlite.MiniSqlite.install();
        Connection c = DriverManager.getConnection("jdbc:minisqlite:" + f.getAbsolutePath());
        try (Statement st = c.createStatement()) { for (String s : Files.readString(Path.of("wallet.sql")).split(";\\s*\\n")) if (!s.isBlank() && !s.trim().startsWith("--")) st.executeUpdate(s); }
        return c;
    }
    static double bal(Connection c, String id) throws SQLException { try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM account WHERE acc_id = ?")) { ps.setString(1, id); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getDouble(1); } } }
    static String state(Connection c) throws SQLException { double a = bal(c, FROM), b = bal(c, TO); return String.format("hostel=%.2f canteen=%.2f total=%.2f", a, b, a + b); }
    static void update(Connection c, String id, double delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE account SET balance = balance + ? WHERE acc_id = ?")) { ps.setDouble(1, delta); ps.setString(2, id); ps.executeUpdate(); }
    }

    /** One transaction: both updates or neither. crashAfterFirst simulates a failure between the two statements. */
    static void transferTx(Connection c, double amt, boolean debitFirst, boolean crashAfterFirst) throws SQLException {
        boolean old = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            update(c, debitFirst ? FROM : TO, debitFirst ? -amt : amt);
            if (crashAfterFirst) throw new IllegalStateException("simulated crash between the two updates");
            update(c, debitFirst ? TO : FROM, debitFirst ? amt : -amt);
            c.commit();
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        finally { c.setAutoCommit(old); }
    }
    /** Two independent statements, each committed by itself (auto-commit ON). */
    static void transferAuto(Connection c, double amt, boolean debitFirst, boolean crashAfterFirst) throws SQLException {
        c.setAutoCommit(true);
        update(c, debitFirst ? FROM : TO, debitFirst ? -amt : amt);
        if (crashAfterFirst) throw new IllegalStateException("simulated crash between the two updates");
        update(c, debitFirst ? TO : FROM, debitFirst ? amt : -amt);
    }
    interface Op { void run(Connection c) throws Exception; }
    static void scenario(String title, boolean tx, Op op) throws Exception {
        File f = File.createTempFile("a5wallet", ".db"); f.delete();
        try (Connection c = fresh(f)) {
            String before = state(c), err = "none";
            try { op.run(c); } catch (Exception e) { err = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage().replaceAll("\\s+\\[.*", "")); }
            System.out.printf("%-58s%n    before: %s%n    error : %s%n    after : %s%n", title, before, err, state(c));
        } finally { f.delete(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== A5.3 Transfer Rs.300 from the hostel wallet to the canteen wallet ===");
        scenario("1. transaction, no failure", true, c -> transferTx(c, 300, true, false));
        scenario("2. TRANSACTION, crash between the two updates", true, c -> transferTx(c, 300, true, true));
        scenario("3. AUTO-COMMIT, crash between the two updates (debit first)", false, c -> transferAuto(c, 300, true, true));
        scenario("4. AUTO-COMMIT, credit first, then the debit fails (balance too low: 2,000)", false, c -> transferAuto(c, 2000, false, false));
        scenario("5. TRANSACTION, credit first, then the debit fails (balance too low: 2,000)", true, c -> transferTx(c, 2000, false, false));

        System.out.println();
        System.out.println("=== Cost: 2,000 transfers of Rs.1 (database on disk, every commit is made durable) ===");
        for (int w = 0; w < 2; w++) timeRun(200, true);
        double tx = timeRun(2000, true), auto = timeRun(2000, false);
        System.out.printf("one transaction per transfer (1 commit)     : %.1f ms in total, %.0f us per transfer%n", tx, tx * 1000 / 2000);
        System.out.printf("two auto-committed updates (2 commits)      : %.1f ms in total, %.0f us per transfer%n", auto, auto * 1000 / 2000);
        System.out.printf("ratio auto-commit / transaction             : %.2f%n", auto / tx);
    }
    static double timeRun(int n, boolean tx) throws Exception {
        File f = File.createTempFile("a5time", ".db"); f.delete();
        try (Connection c = fresh(f)) {
            update(c, FROM, 100000);
            long t = System.nanoTime();
            for (int i = 0; i < n; i++) { if (tx) transferTx(c, 1, true, false); else transferAuto(c, 1, true, false); }
            return (System.nanoTime() - t) / 1e6;
        } finally { f.delete(); }
    }
}
