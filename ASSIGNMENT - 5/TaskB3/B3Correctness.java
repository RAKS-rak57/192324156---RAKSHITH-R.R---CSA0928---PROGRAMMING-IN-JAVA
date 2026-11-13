import java.io.File;
import java.sql.*;
import java.util.*;

/** Task B3.1: insert 10 records with each approach, then verify the table and the column types with a SELECT and ResultSetMetaData. */
public class B3Correctness {
    static String typeName(int t) { for (java.lang.reflect.Field f : Types.class.getFields()) try { if (f.getInt(null) == t) return f.getName(); } catch (Exception e) { } return "?"; }

    public static void main(String[] args) throws Exception {
        List<String[]> ten = PassData.generate(10);
        String[] names = {"Statement (string-built SQL)", "PreparedStatement", "PassLoader (batch + transaction, completed TODOs)"};
        int pass = 0, fail = 0;
        for (int a = 0; a < 3; a++) {
            File f = Db.newFile("correct");
            try (Connection c = Db.fresh(f, "bus_pass.sql")) {
                if (a == 0) PassInserters.statement(c, ten); else if (a == 1) PassInserters.prepared(c, ten); else System.out.println("   PassLoader.loadAll returned " + PassLoader.loadAll(c, ten) + " and auto-commit is back to " + c.getAutoCommit());
                System.out.println("=== " + names[a] + " ===");
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bus_pass ORDER BY pass_id")) {
                    ResultSetMetaData md = rs.getMetaData();
                    StringBuilder cols = new StringBuilder("  columns: ");
                    for (int i = 1; i <= md.getColumnCount(); i++) cols.append(md.getColumnName(i)).append(" ").append(md.getColumnTypeName(i)).append(" (java.sql.Types.").append(typeName(md.getColumnType(i))).append(")").append(i < md.getColumnCount() ? ", " : "");
                    System.out.println(cols);
                    int row = 0; boolean allEqual = true;
                    while (rs.next()) {
                        String[] e = ten.get(row++);
                        boolean ok = rs.getString("pass_id").equals(e[0]) && rs.getString("reg_no").equals(e[1]) && rs.getString("name").equals(e[2])
                                && rs.getInt("route_no") == Integer.parseInt(e[3]) && Math.abs(rs.getDouble("amount") - Double.parseDouble(e[4])) < 0.001;
                        if (!ok) allEqual = false;
                        if (row <= 3) System.out.println("  row " + row + ": " + rs.getString(1) + ", " + rs.getString(2) + ", " + rs.getString(3) + ", " + rs.getInt(4) + ", " + rs.getDouble(5));
                    }
                    boolean good = row == 10 && allEqual;
                    if (good) pass++; else fail++;
                    System.out.println("  " + (good ? "PASS" : "FAIL") + ": " + row + " rows found, every field equals the source value = " + allEqual);
                }
            } finally { f.delete(); }
        }
        System.out.println();
        System.out.println("Result: " + pass + " passed, " + fail + " failed");
    }
}
