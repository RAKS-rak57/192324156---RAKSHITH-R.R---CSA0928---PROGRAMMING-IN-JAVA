package campus.reg.dao;

import campus.reg.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** MySQL implementation (JDBC). Needs mysql-connector-j on the class path. Every method opens and closes its own connection. */
public class JdbcRegistrationDao implements RegistrationDao {
    private static final String SELECT_ROWS =
        "SELECT r.reg_id, r.reg_no, s.name, e.title, r.registered_on "
      + "FROM registration r JOIN student s ON s.reg_no = r.reg_no JOIN event e ON e.event_id = r.event_id ";

    private final String url, user, password;

    public JdbcRegistrationDao(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }

    private Connection open() throws SQLException { return DriverManager.getConnection(url, user, password); }

    @Override
    public List<CampusEvent> findEvents() {
        List<CampusEvent> list = new ArrayList<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT event_id, title, venue, event_date FROM event ORDER BY event_date")) {
            while (rs.next()) list.add(new CampusEvent(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        } catch (SQLException e) { throw new DataAccessException("Cannot read events: " + e.getMessage(), e); }
        return list;
    }

    @Override
    public boolean exists(String regNo, int eventId) {
        String sql = "SELECT COUNT(*) FROM registration WHERE reg_no = ? AND event_id = ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, regNo); ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new DataAccessException("Cannot check registration: " + e.getMessage(), e); }
    }

    @Override
    public void register(Student s, int eventId) throws DuplicateRegistrationException {
        String upsertStudent = "INSERT INTO student(reg_no, name, email) VALUES (?, ?, ?) "
                             + "ON DUPLICATE KEY UPDATE name = VALUES(name), email = VALUES(email)";
        String insertReg = "INSERT INTO registration(reg_no, event_id) VALUES (?, ?)";
        try (Connection c = open()) {
            c.setAutoCommit(false);                                   // student row + registration row: all or nothing
            try (PreparedStatement p1 = c.prepareStatement(upsertStudent); PreparedStatement p2 = c.prepareStatement(insertReg)) {
                p1.setString(1, s.regNo()); p1.setString(2, s.name()); p1.setString(3, s.email());
                p1.executeUpdate();
                p2.setString(1, s.regNo()); p2.setInt(2, eventId);
                p2.executeUpdate();
                c.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                c.rollback();
                if (e.getErrorCode() == 1062)                          // MySQL "duplicate entry": the UNIQUE key caught a race
                    throw new DuplicateRegistrationException("Already registered for this event.");
                throw new DataAccessException("Integrity error: " + e.getMessage(), e);
            } catch (SQLException e) {
                c.rollback();
                throw new DataAccessException("Cannot save registration: " + e.getMessage(), e);
            }
        } catch (SQLException e) { throw new DataAccessException("Database not available: " + e.getMessage(), e); }
    }

    @Override
    public int count(Integer eventId) {
        String sql = "SELECT COUNT(*) FROM registration" + (eventId == null ? "" : " WHERE event_id = ?");
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId != null) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DataAccessException("Cannot count registrations: " + e.getMessage(), e); }
    }

    @Override
    public List<RegistrationRow> page(Integer eventId, int limit, int offset) {
        String sql = SELECT_ROWS + (eventId == null ? "" : "WHERE r.event_id = ? ") + "ORDER BY r.reg_id LIMIT ? OFFSET ?";
        List<RegistrationRow> rows = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (eventId != null) ps.setInt(i++, eventId);
            ps.setInt(i++, limit); ps.setInt(i, offset);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(toRow(rs)); }
        } catch (SQLException e) { throw new DataAccessException("Cannot load registrations: " + e.getMessage(), e); }
        return rows;
    }

    @Override
    public List<RegistrationRow> findAll(Integer eventId) {
        String sql = SELECT_ROWS + (eventId == null ? "" : "WHERE r.event_id = ? ") + "ORDER BY r.reg_id";
        List<RegistrationRow> rows = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId != null) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(toRow(rs)); }
        } catch (SQLException e) { throw new DataAccessException("Cannot read registrations: " + e.getMessage(), e); }
        return rows;
    }

    private static RegistrationRow toRow(ResultSet rs) throws SQLException {
        return new RegistrationRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
    }
}
