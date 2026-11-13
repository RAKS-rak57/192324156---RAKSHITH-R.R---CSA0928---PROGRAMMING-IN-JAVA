import java.sql.*;
/** ALTERNATIVE design: SQL inside the service classes (every class repeats open / prepare / map / close). */
public class ScatteredStudentService {
    private final AbstractJdbcDao.ConnectionFactory f;
    public ScatteredStudentService(AbstractJdbcDao.ConnectionFactory f) { this.f = f; }
    public Student find(String regNo) throws SQLException {
        try (Connection c = f.open(); PreparedStatement ps = c.prepareStatement("SELECT reg_no, name, email, year_no FROM student WHERE reg_no = ?")) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? new Student(rs.getString("reg_no"), rs.getString("name"), rs.getString("email"), rs.getInt("year_no")) : null; }
        }
    }
    public int rename(String regNo, String name) throws SQLException {
        try (Connection c = f.open(); PreparedStatement ps = c.prepareStatement("UPDATE student SET name = ? WHERE reg_no = ?")) { ps.setString(1, name); ps.setString(2, regNo); return ps.executeUpdate(); }
    }
}
