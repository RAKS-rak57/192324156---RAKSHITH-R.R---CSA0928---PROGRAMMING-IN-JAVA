import java.sql.*;
public class ScatteredRegistrationScreen {
    private final AbstractJdbcDao.ConnectionFactory f;
    public ScatteredRegistrationScreen(AbstractJdbcDao.ConnectionFactory f) { this.f = f; }
    public void register(String regNo, String name, String email, int year) throws SQLException {
        try (Connection c = f.open(); PreparedStatement ps = c.prepareStatement("INSERT INTO student(reg_no, name, email, year_no) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, regNo); ps.setString(2, name); ps.setString(3, email); ps.setInt(4, year); ps.executeUpdate();
        }
    }
    public boolean exists(String regNo) throws SQLException {
        try (Connection c = f.open(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM student WHERE reg_no = ?")) { ps.setString(1, regNo); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; } }
    }
}
