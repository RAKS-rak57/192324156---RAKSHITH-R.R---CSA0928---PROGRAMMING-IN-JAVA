import java.sql.*;
import java.util.List;

/** The insert strategies compared in Task B3. The caller decides the commit mode (autocommit on/off) before calling. */
public class PassInserters {
    /** (a) Statement: a NEW SQL string for every row (the database parses every statement again). */
    public static int statement(Connection c, List<String[]> passes) throws SQLException {
        try (Statement st = c.createStatement()) {
            for (String[] p : passes)
                st.executeUpdate("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES ('" + p[0] + "', '" + p[1] + "', '" + p[2] + "', " + p[3] + ", " + p[4] + ")");
        }
        return passes.size();
    }

    /** (b) PreparedStatement: parsed once, then bind + execute for every row. */
    public static int prepared(Connection c, List<String[]> passes) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)")) {
            for (String[] p : passes) { bind(ps, p); ps.executeUpdate(); }
        }
        return passes.size();
    }

    /** (c) PreparedStatement + addBatch()/executeBatch() every batchSize rows (commit mode decided by the caller). */
    public static int batch(Connection c, List<String[]> passes, int batchSize) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)")) {
            int n = 0;
            for (String[] p : passes) { bind(ps, p); ps.addBatch(); if (++n % batchSize == 0) ps.executeBatch(); }
            ps.executeBatch();
            return n;
        }
    }

    /** Alternative: ONE statement inserts b rows:  INSERT ... VALUES (?,?,?,?,?),(?,?,?,?,?),... */
    public static int multiRow(Connection c, List<String[]> passes, int b) throws SQLException {
        String one = "(?, ?, ?, ?, ?)";
        String head = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES ";
        int n = passes.size(), full = n / b;
        if (full > 0) {
            String sql = head + String.join(", ", java.util.Collections.nCopies(b, one));
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int k = 0; k < full; k++) { for (int j = 0; j < b; j++) bindAt(ps, passes.get(k * b + j), j * 5); ps.executeUpdate(); }
            }
        }
        int rest = n - full * b;
        if (rest > 0) {
            String sql = head + String.join(", ", java.util.Collections.nCopies(rest, one));
            try (PreparedStatement ps = c.prepareStatement(sql)) { for (int j = 0; j < rest; j++) bindAt(ps, passes.get(full * b + j), j * 5); ps.executeUpdate(); }
        }
        return n;
    }

    static void bind(PreparedStatement ps, String[] p) throws SQLException { bindAt(ps, p, 0); }
    static void bindAt(PreparedStatement ps, String[] p, int off) throws SQLException {
        ps.setString(off + 1, p[0]); ps.setString(off + 2, p[1]); ps.setString(off + 3, p[2]); ps.setInt(off + 4, Integer.parseInt(p[3])); ps.setDouble(off + 5, Double.parseDouble(p[4]));
    }
}
