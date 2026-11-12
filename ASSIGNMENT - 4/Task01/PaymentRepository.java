import java.sql.SQLException;
import java.util.List;

/** Data access for fee payments. The service depends on this interface, not on JDBC. */
public interface PaymentRepository {
    /** Stores a payment and returns its payment id. */
    long save(String regNo, double amount) throws SQLException;
    /** Returns one text line per payment made by the student. */
    List<String> history(String regNo) throws SQLException;
}
