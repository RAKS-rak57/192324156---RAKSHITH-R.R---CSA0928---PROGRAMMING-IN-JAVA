import java.sql.*;
public class ScatteredReportScreen {
    private final AbstractJdbcDao.ConnectionFactory f;
    public ScatteredReportScreen(AbstractJdbcDao.ConnectionFactory f) { this.f = f; }
    public String studentList() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection c = f.open(); PreparedStatement ps = c.prepareStatement("SELECT reg_no, name FROM student ORDER BY reg_no"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) sb.append(rs.getString("reg_no")).append(' ').append(rs.getString("name")).append('\n');
        }
        return sb.toString();
    }
}
