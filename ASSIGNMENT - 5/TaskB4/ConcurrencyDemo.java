import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Task B4.3 / alternative: several counters issue passes at the same time. Who gives each new pass a UNIQUE number? */
public class ConcurrencyDemo {
    static final int COUNTERS = 4, PER_COUNTER = 500;
    static final Object LOCK = new Object();

    public static void main(String[] args) throws Exception {
        minisqlite.MiniSqlite.install();
        System.out.println("=== " + COUNTERS + " counters issue " + PER_COUNTER + " passes each at the same moment (expected: " + COUNTERS * PER_COUNTER + " passes, all numbers different) ===");

        // ---- (1) text file, "number = lines in the file + 1", no coordination ----
        for (int attempt = 1; attempt <= 3; attempt++) {
            Path f = Files.createTempFile("issue", ".txt");
            run((counter, i) -> { long next = Files.readAllLines(f).size() + 1; Files.writeString(f, "P" + next + ",counter" + counter + "\n", StandardOpenOption.APPEND); });
            report("(1) text file, no coordination (run " + attempt + ")", Files.readAllLines(f));
            Files.delete(f);
        }
        // ---- (2) text file with a lock inside ONE program (all counters are threads of the same JVM) ----
        Path f2 = Files.createTempFile("issue", ".txt");
        run((counter, i) -> { synchronized (LOCK) { long next = Files.readAllLines(f2).size() + 1; Files.writeString(f2, "P" + next + ",counter" + counter + "\n", StandardOpenOption.APPEND); } });
        report("(2) text file + synchronized (ONE program only)", Files.readAllLines(f2));
        Files.delete(f2);

        // ---- (3) database: the database creates the number (AUTO_INCREMENT / INTEGER PRIMARY KEY) ----
        File db = File.createTempFile("issued", ".db"); db.delete();
        try (Connection c = DriverManager.getConnection("jdbc:minisqlite:" + db.getAbsolutePath()); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE issued (pass_no INTEGER PRIMARY KEY, counter_no INT NOT NULL)");
        }
        List<Long> keys = Collections.synchronizedList(new ArrayList<>());
        long t = System.nanoTime();
        runDb(db, keys);
        double ms = (System.nanoTime() - t) / 1e6;
        Set<Long> distinct = new HashSet<>(keys);
        int rows;
        try (Connection c = DriverManager.getConnection("jdbc:minisqlite:" + db.getAbsolutePath()); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM issued")) { rs.next(); rows = rs.getInt(1); }
        System.out.printf("(3) database, each counter has its own connection and the DATABASE assigns the number%n      passes issued: %d | rows in the table: %d | distinct numbers: %d | duplicates: %d | time %.0f ms%n", keys.size(), rows, distinct.size(), keys.size() - distinct.size(), ms);
        db.delete();
    }

    interface Issue { void issue(int counter, int i) throws Exception; }
    static void run(Issue body) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(COUNTERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int c = 0; c < COUNTERS; c++) { final int counter = c + 1; fs.add(ex.submit(() -> { go.await(); for (int i = 0; i < PER_COUNTER; i++) body.issue(counter, i); return null; })); }
        go.countDown();
        for (Future<?> f : fs) f.get();
        ex.shutdown();
    }
    static void runDb(File db, List<Long> keys) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(COUNTERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int c = 0; c < COUNTERS; c++) {
            final int counter = c + 1;
            fs.add(ex.submit(() -> {
                try (Connection con = DriverManager.getConnection("jdbc:minisqlite:" + db.getAbsolutePath())) {
                    try (Statement st = con.createStatement()) { st.execute("PRAGMA busy_timeout = 20000"); }          // wait for the other counters instead of failing
                    go.await();
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO issued(counter_no) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                        for (int i = 0; i < PER_COUNTER; i++) { ps.setInt(1, counter); ps.executeUpdate(); try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); keys.add(rs.getLong(1)); } }
                    }
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) f.get();
        ex.shutdown();
    }
    static void report(String title, List<String> lines) {
        Set<String> ids = new HashSet<>(); int bad = 0;
        for (String l : lines) { if (!l.matches("P\\d+,counter\\d")) bad++; ids.add(l.split(",")[0]); }
        System.out.printf("%s%n      lines: %d | distinct pass numbers: %d | duplicate numbers: %d | damaged lines: %d%n", title, lines.size(), ids.size(), lines.size() - ids.size(), bad);
    }
}
