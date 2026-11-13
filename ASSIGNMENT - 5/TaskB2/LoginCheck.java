import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** The unsafe login of Task A5.2, the PreparedStatement redesign, and the two weaker "fixes" analysed in B2. */
public class LoginCheck {
    /** Result: the SQL text that was run (for the report) and the ids that were returned. */
    public static class Result { public String sql; public List<String> ids = new ArrayList<>(); public boolean accepted() { return !ids.isEmpty(); } }

    // ---------- (1) UNSAFE: the original code ----------
    public static Result unsafe(Connection c, String id, String pwd) throws SQLException {
        Result r = new Result();
        r.sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(r.sql)) { while (rs.next()) r.ids.add(rs.getString("id")); }
        return r;
    }

    // ---------- (2) SAFE: PreparedStatement ----------
    public static Result safe(Connection c, String id, String pwd) throws SQLException {
        Result r = new Result();
        r.sql = "SELECT * FROM users WHERE id = ? AND pwd = ?     [id and pwd sent as DATA]";
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE id = ? AND pwd = ?")) {
            ps.setString(1, id); ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) r.ids.add(rs.getString("id")); }
        }
        return r;
    }

    // ---------- (3) WEAKER: escape the quote characters, still build the SQL string ----------
    public static String escapeQuotes(String s) { return s.replace("'", "''"); }
    public static Result escaped(Connection c, String id, String pwd) throws SQLException { return unsafe(c, escapeQuotes(id), escapeQuotes(pwd)); }

    // ---------- (4) WEAKER: validate with a regular expression, still build the SQL string ----------
    static final Pattern SAFE_TEXT = Pattern.compile("[A-Za-z0-9_]{1,20}");
    public static Result regex(Connection c, String id, String pwd) throws SQLException {
        if (!SAFE_TEXT.matcher(id).matches() || !SAFE_TEXT.matcher(pwd).matches()) { Result r = new Result(); r.sql = "(rejected by the validator, no SQL sent)"; return r; }
        return unsafe(c, id, pwd);
    }

    // ---------- a numeric context: no quotes at all, so quote-escaping cannot help ----------
    public static Result findByNumber(Connection c, String userNoText, boolean useEscaping) throws SQLException {
        Result r = new Result();
        String text = useEscaping ? escapeQuotes(userNoText) : userNoText;
        r.sql = "SELECT * FROM users WHERE user_no = " + text;
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(r.sql)) { while (rs.next()) r.ids.add(rs.getString("id")); }
        return r;
    }
}
