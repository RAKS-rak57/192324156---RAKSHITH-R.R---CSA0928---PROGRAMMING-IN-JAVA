import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.sql.*;
import java.util.List;

public class A3Demo {
    static long sink = 0;
    static long allocated() { com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean(); return b.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    static int occurrences(String text, String word) { int n = 0, i = 0; while ((i = text.indexOf(word, i)) >= 0) { n++; i += word.length(); } return n; }

    public static void main(String[] args) throws Exception {
        minisqlite.MiniSqlite.install();
        File db = File.createTempFile("a3demo", ".db"); db.delete();
        String url = "jdbc:minisqlite:" + db.getAbsolutePath();
        AbstractJdbcDao.ConnectionFactory f = () -> DriverManager.getConnection(url);
        try (Connection c = f.open(); Statement st = c.createStatement()) { for (String sql : Files.readString(Path.of("student.sql")).split(";")) if (!sql.isBlank()) st.executeUpdate(sql); }

        System.out.println("=== A3.2 One generic design, three entities ===");
        StudentDao students = new StudentDao(f);
        students.insert(new Student("192311001", "Arun", "arun@example.com", 2));
        students.insert(new Student("192311002", "Divya", "divya@example.com", 3));
        System.out.println("findById(192311002)      : " + students.findById("192311002"));
        System.out.println("findById(000000000)      : " + students.findById("000000000") + "   (no such row)");
        System.out.println("update Arun's year       : rows changed = " + students.update(new Student("192311001", "Arun", "arun@example.com", 3)));
        System.out.println("findAll()                : " + students.findAll());
        System.out.println("delete(192311002)        : rows deleted = " + students.delete("192311002") + ", remaining = " + students.findAll().size());
        CourseDao courses = new CourseDao(f);
        courses.insert(new CourseDao.Course("CSA09", "Programming in Java", 4));
        System.out.println("CourseDao.findAll()      : " + courses.findAll());
        MarksDao marks = new MarksDao(f);
        marks.insert(new MarksDao.Mark(1, "192311001", "CSA09", 88)); marks.insert(new MarksDao.Mark(2, "192311001", "CSA10", 76));
        System.out.println("MarksDao.findAll()       : " + marks.findAll());

        System.out.println();
        System.out.println("=== Alternative: SQL written inside service / GUI classes ===");
        ScatteredStudentService alt = new ScatteredStudentService(f);
        System.out.println("ScatteredStudentService.find(192311001) : " + alt.find("192311001") + "   (same result)");

        // ----- how many places change when a column of the STUDENT table is renamed? (counted from the real source files) -----
        System.out.println();
        System.out.println("=== Rename a column of the student table: SQL statements that mention the table, and in how many classes ===");
        String[] all = {"StudentDao.java", "CourseDao.java", "MarksDao.java", "ScatteredStudentService.java", "ScatteredReportScreen.java", "ScatteredRegistrationScreen.java", "ScatteredMarksService.java"};
        int daoStatements = 0, daoClasses = 0, altStatements = 0, altClasses = 0;
        for (String fn : all) {
            int n = studentStatements(Files.readString(Path.of(fn)));
            if (n == 0) continue;
            boolean generic = !fn.startsWith("Scattered");
            System.out.println("  " + (generic ? "generic DAO design : " : "scattered design   : ") + fn + " -> " + n + " statement(s) on the student table");
            if (generic) { daoStatements += n; daoClasses++; } else { altStatements += n; altClasses++; }
        }
        System.out.println("  generic DAO : " + daoStatements + " statements, all in " + daoClasses + " class (StudentDao)");
        System.out.println("  scattered   : " + altStatements + " statements spread over " + altClasses + " classes (every service / screen that touches the table)");

        // ----- cost per operation -----
        System.out.println();
        System.out.println("=== Cost per findById (new connection each time, as in the original StudentDB class) ===");
        ScatteredStudentService direct = alt;
        for (int i = 0; i < 300; i++) { sink += students.findById("192311001") != null ? 1 : 0; sink += direct.find("192311001") != null ? 1 : 0; }
        int n = 3000; double[] us = new double[2]; long[] bytes = new long[2];
        for (int d = 0; d < 2; d++) {
            double best = 1e18;
            for (int r = 0; r < 5; r++) {
                long a0 = allocated(), t0 = System.nanoTime();
                for (int i = 0; i < n; i++) sink += (d == 0 ? students.findById("192311001") : direct.find("192311001")) != null ? 1 : 0;
                best = Math.min(best, (System.nanoTime() - t0) / 1e3 / n);
                if (r == 0) bytes[d] = (allocated() - a0) / n;
            }
            us[d] = best;
        }
        System.out.printf("generic Dao<Student,String>.findById : %.1f microseconds per call, %,d bytes allocated per call%n", us[0], bytes[0]);
        System.out.printf("SQL inside the service class         : %.1f microseconds per call, %,d bytes allocated per call%n", us[1], bytes[1]);
        System.out.println("(checksum " + sink + ")");
        db.delete();
    }

    /** counts SQL statements (in code lines, not comments) that read or write the student table */
    static int studentStatements(String src) {
        int n = 0;
        for (String line : src.split("\n")) {
            String t = line.trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(FROM|INTO|UPDATE|JOIN) student\\b").matcher(line);
            while (m.find()) n++;
        }
        return n;
    }
}
