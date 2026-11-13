import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.CRC32;

/** Task B4.1 / B4.2: migrate an existing pass file that contains some bad lines and print the failure report. */
public class MigrationDemo {
    static String withCrc(String body) { CRC32 c = new CRC32(); c.update(body.getBytes()); return body + "," + Long.toHexString(c.getValue()); }

    public static void main(String[] args) throws Exception {
        minisqlite.MiniSqlite.install();
        // ----- the existing file: 8,000 good passes + 14 bad lines of different kinds -----
        Path file = Files.createTempFile("existing_passes", ".txt");
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 8000; i++) lines.add(new BusPass("P" + (1001 + i), String.format("19%07d", i), "Student " + (i % 500), 1 + i % 40, "Stop " + (i % 25), LocalDate.of(2027, 6, 30), 3000 + i % 2000).toLine());
        String[][] bad = {{"500", "P1500,192311500,Short"}, {"1200", withCrc("P9001,192319001,Ravi,3,Main Gate,2027-06-30,forty")}, {"2000", withCrc("P9002,192319002,Ravi,abc,Main Gate,2027-06-30,4500.00")},
            {"3000", withCrc("P9003,192319003,Ravi,3,Main Gate,2027-13-45,4500.00")}, {"3500", ""}, {"4000", lines.get(10).replace("Student", "Studant")},
            {"4500", withCrc("P9004,192319004,Ravi,3,Main Gate,2027-06-30,-50.00")}, {"5000", withCrc("P9005,192319005," + "N".repeat(70) + ",3,Main Gate,2027-06-30,4500.00")},
            {"5500", lines.get(7)}, {"6000", lines.get(8)}, {"6500", lines.get(9)}, {"7000", lines.get(10)}, {"7500", withCrc("P9006,192319006,Kumar, Meena,3,Main Gate,2027-06-30,4500.00")}, {"7900", lines.get(11).substring(0, 40)}};
        for (int k = bad.length - 1; k >= 0; k--) lines.add(Integer.parseInt(bad[k][0]), bad[k][1]);       // insert from the back so positions stay valid
        Files.write(file, lines);
        System.out.println("Existing file: " + lines.size() + " lines (8,000 good, 14 deliberately bad: 4 of them repeat an earlier pass id)");

        File dbFile = File.createTempFile("campus", ".db"); dbFile.delete();
        try (Connection c = DriverManager.getConnection("jdbc:minisqlite:" + dbFile.getAbsolutePath())) {
            try (Statement st = c.createStatement()) { for (String s : Files.readString(Path.of("bus_pass_full.sql")).split(";\\s*\\n")) if (!s.isBlank() && !s.trim().startsWith("--")) st.executeUpdate(s); }
            System.out.println();
            System.out.println("=== B4.2 DatabaseMetaData report (before the migration) ===");
            DbInspector.report(c);

            System.out.println();
            System.out.println("=== B4.1 Migration: batches of 1,000 inside one transaction ===");
            MigrationReport rep = Migration.run(file, c, 1);
            System.out.println("lines read       : " + rep.linesRead);
            System.out.println("loaded           : " + rep.loaded);
            System.out.println("failed validation: " + rep.rejected + "  (of which duplicate pass ids: " + rep.duplicates + ")");
            System.out.println("time             : " + rep.millis + " ms");
            System.out.println("failure report, grouped by reason:");
            for (Map.Entry<String, Integer> e : rep.reasons.entrySet()) System.out.printf("   %3d x %s%n", e.getValue(), e.getKey());
            System.out.println("failure report, line by line:");
            for (String s : rep.rejectLines) System.out.println("   " + s);

            System.out.println();
            System.out.println("=== Verification: the database must contain exactly what was accepted ===");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*), SUM(amount), COUNT(DISTINCT pass_id) FROM bus_pass_full")) {
                rs.next();
                double expectedSum = 0; for (int i = 0; i < 8000; i++) expectedSum += 3000 + i % 2000;
                System.out.println("rows in database = " + rs.getInt(1) + " (expected " + rep.loaded + "), distinct pass ids = " + rs.getInt(3) + ", sum of amounts = " + rs.getDouble(2) + " (expected " + expectedSum + ")");
                System.out.println((rs.getInt(1) == rep.loaded && rs.getInt(1) == 8000 && Math.abs(rs.getDouble(2) - expectedSum) < 0.01) ? "PASS: file and database agree" : "FAIL");
            }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT route_no, COUNT(*) FROM bus_pass_full WHERE route_no <= 3 GROUP BY route_no ORDER BY route_no")) {
                StringBuilder sb = new StringBuilder("passes per route (routes 1-3): ");
                while (rs.next()) sb.append("route ").append(rs.getInt(1)).append(" = ").append(rs.getInt(2)).append("   ");
                System.out.println(sb);
            }
            System.out.println();
            System.out.println("=== The same report AFTER the migration (tables present) ===");
            DbInspector.report(c);
        }
        dbFile.delete(); Files.delete(file);
    }
}
