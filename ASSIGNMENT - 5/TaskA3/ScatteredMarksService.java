import java.sql.*;
public class ScatteredMarksService {
    private final AbstractJdbcDao.ConnectionFactory f;
    public ScatteredMarksService(AbstractJdbcDao.ConnectionFactory f) { this.f = f; }
    public double average(String regNo) throws SQLException {
        try (Connection c = f.open(); PreparedStatement ps = c.prepareStatement("SELECT AVG(m.marks) FROM marks m JOIN student s ON s.reg_no = m.reg_no WHERE s.reg_no = ?")) {
            ps.setString(1, regNo); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getDouble(1); }
        }
    }
    public int deleteMarksOf(String regNo) throws SQLException {
        try (Connection c = f.open(); PreparedStatement ps = c.prepareStatement("DELETE FROM marks WHERE reg_no = ?")) { ps.setString(1, regNo); return ps.executeUpdate(); }
    }
}
