import java.io.File;
import java.nio.file.*;
import java.sql.*;

/** Creates a fresh test database (a new file) with the bus_pass table, using the SQL script. */
public class Db {
    public static String dir = System.getProperty("db.dir", "/tmp");
    public static File newFile(String tag) { File f = new File(dir, "b3_" + tag + "_" + System.nanoTime() + ".db"); f.deleteOnExit(); return f; }
    public static Connection fresh(File f, String scriptPath) throws Exception {
        minisqlite.MiniSqlite.install();
        Connection c = DriverManager.getConnection("jdbc:minisqlite:" + f.getAbsolutePath());
        try (Statement st = c.createStatement()) { for (String s : Files.readString(Path.of(scriptPath)).split(";\\s*\\n")) if (!s.isBlank() && !s.trim().startsWith("--")) st.executeUpdate(s); }
        return c;
    }
    public static int count(Connection c) throws SQLException { try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) { rs.next(); return rs.getInt(1); } }
}
