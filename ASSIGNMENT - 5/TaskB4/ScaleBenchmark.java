import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Tasks B4.3 and the alternative: 12 campuses, N = 800,000 passes (100 x 8,000).
 *   (A) one text file            (B) one serialized file per campus            (C) one database (indexed)
 */
public class ScaleBenchmark {
    static long sink = 0;
    static final int N = Integer.getInteger("n", 800_000), CAMPUSES = 12;
    static BusPass make(int i) { return new BusPass("P" + (100001 + i), String.format("19%07d", i), "Student " + (i % 1000), 1 + i % 40, "Stop " + (i % 25), LocalDate.of(2027, 6, 30), 3000 + i % 2000); }
    static long used() { for (int i = 0; i < 3; i++) System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
    static double ms(long t0) { return (System.nanoTime() - t0) / 1e6; }

    public static void main(String[] args) throws Exception {
        minisqlite.MiniSqlite.install();
        PrintWriter csv = new PrintWriter("scale_results.csv");
        csv.println("design,measure,value,unit");
        System.out.println("=== 12 campuses, N = " + String.format("%,d", N) + " passes ===");
        Path dir = Files.createTempDirectory("scale");
        String lastId = "P" + (100001 + N - 1), midReg = String.format("19%07d", N / 2);

        // ---------------- (A) one text file ----------------
        Path text = dir.resolve("passes.txt");
        long t = System.nanoTime();
        try (BufferedWriter w = Files.newBufferedWriter(text)) { for (int i = 0; i < N; i++) w.write(make(i).toLine() + "\n"); }
        double writeA = ms(t);
        BusPassStore store = new BusPassStore(text);
        for (int i = 0; i < 2; i++) sink += store.find(lastId) != null ? 1 : 0;
        t = System.nanoTime(); for (int i = 0; i < 3; i++) sink += store.find(lastId) != null ? 1 : 0; double findA = ms(t) / 3;
        t = System.nanoTime(); for (int i = 0; i < 3; i++) sink += store.listByRoute(7).size(); double routeA = ms(t) / 3;
        long bytesA = Files.size(text);
        System.out.println();
        System.out.println("(A) ONE TEXT FILE");
        System.out.printf("    file size %,d bytes | write %.0f ms | find(last pass) %.0f ms | list one route (%d passes) %.0f ms%n", bytesA, writeA, findA, store.listByRoute(7).size(), routeA);
        csv.printf("text file,size,%d,bytes%ntext file,find one pass,%.1f,ms%ntext file,list one route,%.1f,ms%n", bytesA, findA, routeA);

        // ---------------- (B) one serialized file per campus ----------------
        List<List<BusPass>> perCampus = new ArrayList<>();
        for (int c = 0; c < CAMPUSES; c++) perCampus.add(new ArrayList<>());
        for (int i = 0; i < N; i++) perCampus.get(i % CAMPUSES).add(make(i));
        t = System.nanoTime(); long bytesB = 0;
        for (int c = 0; c < CAMPUSES; c++) {
            Path f = dir.resolve("campus" + (c + 1) + ".ser");
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(f)))) { o.writeObject(perCampus.get(c)); }
            bytesB += Files.size(f);
        }
        double writeB = ms(t);
        perCampus = null;
        long before = used();
        t = System.nanoTime();
        List<List<BusPass>> loaded = new ArrayList<>();
        for (int c = 0; c < CAMPUSES; c++) try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(dir.resolve("campus" + (c + 1) + ".ser"))))) { @SuppressWarnings("unchecked") List<BusPass> l = (List<BusPass>) in.readObject(); loaded.add(l); }
        double loadB = ms(t);
        double heapB = (used() - before) / 1048576.0;
        t = System.nanoTime(); for (int r = 0; r < 5; r++) for (List<BusPass> l : loaded) for (BusPass p : l) if (p.passId.equals(lastId)) sink++; double searchB = ms(t) / 5;
        t = System.nanoTime(); int cnt = 0; for (List<BusPass> l : loaded) for (BusPass p : l) if (p.route == 7) cnt++; double routeB = ms(t);
        t = System.nanoTime(); Map<String, BusPass> index = new HashMap<>(2 * N); for (List<BusPass> l : loaded) for (BusPass p : l) index.put(p.passId, p); double buildIdx = ms(t);
        double heapIdx = (used() - before) / 1048576.0 - heapB;
        t = System.nanoTime(); for (int i = 0; i < 100000; i++) sink += index.get("P" + (100001 + (i * 7919) % N)).route; double lookupB = (System.nanoTime() - t) / 1e5;
        System.out.println();
        System.out.println("(B) ONE SERIALIZED FILE PER CAMPUS (12 files, each file read by one campus program)");
        System.out.printf("    total size %,d bytes | write all %.0f ms | LOAD all 12 files %.0f ms | heap needed %.0f MB%n", bytesB, writeB, loadB, heapB);
        System.out.printf("    search ALL passes for one id (scan the 12 lists in memory) %.1f ms | one route %.1f ms (%d passes)%n", searchB, routeB, cnt);
        System.out.printf("    with an extra HashMap index: build %.0f ms, +%.0f MB heap, then %.0f ns per lookup%n", buildIdx, heapIdx, lookupB);
        csv.printf("per-campus serialization,size,%d,bytes%nper-campus serialization,load all,%.1f,ms%nper-campus serialization,heap,%.1f,MB%nper-campus serialization,search all for one id,%.1f,ms%nper-campus serialization,list one route,%.1f,ms%n", bytesB, loadB, heapB, searchB, routeB);
        loaded = null; index = null;

        // ---------------- (C) one database ----------------
        File dbFile = new File(dir.toFile(), "campus.db");
        try (Connection c = DriverManager.getConnection("jdbc:minisqlite:" + dbFile.getAbsolutePath())) {
            try (Statement st = c.createStatement()) { for (String s : Files.readString(Path.of("bus_pass_full.sql")).split(";\\s*\\n")) if (!s.isBlank() && !s.trim().startsWith("--")) st.executeUpdate(s); }
            t = System.nanoTime();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(Migration.INSERT)) {
                for (int i = 0; i < N; i++) {
                    BusPass p = make(i);
                    ps.setString(1, p.passId); ps.setString(2, p.regNo); ps.setString(3, p.name); ps.setInt(4, p.route); ps.setString(5, p.stop); ps.setString(6, p.validity.toString()); ps.setDouble(7, p.amount); ps.setInt(8, 1 + i % CAMPUSES);
                    ps.addBatch(); if ((i + 1) % 1000 == 0) ps.executeBatch();
                }
                ps.executeBatch();
            }
            c.commit(); c.setAutoCommit(true);
            double load = ms(t);
            long bytesC = dbFile.length();
            double pk = timeQuery(c, "SELECT * FROM bus_pass_full WHERE pass_id = ?", lastId, 3000);
            double regNoIdx = timeQuery(c, "SELECT * FROM bus_pass_full WHERE reg_no = ?", midReg, 5);
            t = System.nanoTime(); try (Statement st = c.createStatement()) { for (String s : Files.readString(Path.of("bus_pass_indexes.sql")).split(";\\s*\\n")) if (!s.isBlank()) st.executeUpdate(s); } double idxBuild = ms(t);
            long bytesIdx = dbFile.length();
            double regIdx = timeQuery(c, "SELECT * FROM bus_pass_full WHERE reg_no = ?", midReg, 3000);
            t = System.nanoTime(); int rows = 0; try (PreparedStatement ps = c.prepareStatement("SELECT pass_id, name FROM bus_pass_full WHERE route_no = ?")) { ps.setInt(1, 7); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { rows++; sink += rs.getString(1).length(); } } } double routeC = ms(t);
            t = System.nanoTime(); int campusCount; try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass_full WHERE campus_id = 5")) { rs.next(); campusCount = rs.getInt(1); } double campusC = ms(t);
            System.out.println();
            System.out.println("(C) ONE DATABASE (table bus_pass_full, campus_id column)");
            System.out.printf("    load all rows (batches of 1,000, one transaction) %.0f ms | database size %,d bytes (%,d with indexes, index build %.0f ms)%n", load, bytesC, bytesIdx, idxBuild);
            System.out.printf("    find one pass by pass_id (PRIMARY KEY index) %.3f ms | by reg_no WITHOUT index %.1f ms | by reg_no WITH index %.3f ms%n", pk, regNoIdx, regIdx);
            System.out.printf("    list one route (%,d rows) %.0f ms (with index on route_no) | count of one campus (%,d) %.0f ms%n", rows, routeC, campusCount, campusC);
            csv.printf("database,size,%d,bytes%ndatabase,load all,%.1f,ms%ndatabase,find by primary key,%.4f,ms%ndatabase,find by reg_no without index,%.2f,ms%ndatabase,find by reg_no with index,%.4f,ms%ndatabase,list one route,%.1f,ms%n", bytesC, load, pk, regNoIdx, regIdx, routeC);
        }
        csv.close();
        System.out.println("(checksum " + sink + ")");
        try (var s = Files.walk(dir)) { s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
    }

    static double timeQuery(Connection c, String sql, String arg, int reps) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < Math.min(reps, 3); i++) { ps.setString(1, arg); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) sink++; } }
            long t = System.nanoTime();
            for (int i = 0; i < reps; i++) { ps.setString(1, arg); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) sink++; } }
            return (System.nanoTime() - t) / 1e6 / reps;
        }
    }
}
