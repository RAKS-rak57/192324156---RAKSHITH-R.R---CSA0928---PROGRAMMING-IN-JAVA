import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC implementation. Uses PreparedStatement (no SQL injection) and try-with-resources (connection always closed). */
public class JdbcPaymentRepository implements PaymentRepository {
    private final String url, user, password;

    public JdbcPaymentRepository(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }

    @Override
    public long save(String regNo, double amount) throws SQLException {
        String sql = "INSERT INTO fees(reg_no, amount) VALUES (?, ?)";
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, regNo);                 // the value is sent as DATA, never as SQL text
            ps.setDouble(2, amount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    @Override
    public List<String> history(String regNo) throws SQLException {
        String sql = "SELECT payment_id, amount, paid_on FROM fees WHERE reg_no = ? ORDER BY payment_id";
        List<String> lines = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(String.format("RCPT-%05d  Rs.%.2f  %s", rs.getLong(1), rs.getDouble(2), rs.getTimestamp(3)));
                }
            }
        }
        return lines;
    }
}
