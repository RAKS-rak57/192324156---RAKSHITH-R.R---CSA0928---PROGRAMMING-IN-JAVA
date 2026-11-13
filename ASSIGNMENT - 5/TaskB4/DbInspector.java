import java.sql.*;

/** Task B4.2: report the database product, the driver version and the tables, using DatabaseMetaData. */
public class DbInspector {
    public static void report(Connection c) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        System.out.println("Database product  : " + md.getDatabaseProductName() + "  version " + md.getDatabaseProductVersion());
        System.out.println("Driver            : " + md.getDriverName() + "  version " + md.getDriverVersion());
        System.out.println("Tables present    :");
        try (ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) System.out.println("   " + rs.getString("TABLE_NAME") + "  (" + rs.getString("TABLE_TYPE") + ")");
        }
    }
}
