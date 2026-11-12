import java.sql.*;
import java.util.*;

/** MySQL version of the repository (compile-checked; run it with the MySQL Connector/J jar on the class path). */
public class JdbcAppointmentRepository implements AppointmentRepository {
    private final String url, user, password;
    public JdbcAppointmentRepository(String url, String user, String password) { this.url = url; this.user = user; this.password = password; }

    private Connection open() throws SQLException { return DriverManager.getConnection(url, user, password); }

    @Override public Map<String, List<Doctor>> loadAllDoctors() {
        String sql = "SELECT d.doctor_id, d.name, p.name FROM doctor d JOIN department p ON p.dept_id = d.dept_id ORDER BY p.name, d.name";
        Map<String, List<Doctor>> map = new TreeMap<>();
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.computeIfAbsent(rs.getString(3), k -> new ArrayList<>()).add(new Doctor(rs.getString(1), rs.getString(2), rs.getString(3)));
        } catch (SQLException e) { throw new IllegalStateException("Cannot load doctors: " + e.getMessage(), e); }
        return map;
    }

    @Override public List<Doctor> findDoctors(String department) {
        String sql = "SELECT d.doctor_id, d.name FROM doctor d JOIN department p ON p.dept_id = d.dept_id WHERE p.name = ? ORDER BY d.name";
        List<Doctor> list = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, department);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(new Doctor(rs.getString(1), rs.getString(2), department)); }
        } catch (SQLException e) { throw new IllegalStateException("Cannot load doctors: " + e.getMessage(), e); }
        return list;
    }

    @Override public List<String> slots() { return List.of("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30"); }

    @Override public boolean isSlotTaken(String doctorId, String slot) {
        String sql = "SELECT COUNT(*) FROM appointment WHERE doctor_id = ? AND appt_date = CURDATE() AND slot_time = ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, doctorId); ps.setString(2, slot);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new IllegalStateException("Cannot check slot: " + e.getMessage(), e); }
    }

    @Override public void save(String patientId, Doctor doctor, String slot) {
        String sql = "INSERT INTO appointment(patient_id, doctor_id, appt_date, slot_time) VALUES (?, ?, CURDATE(), ?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patientId); ps.setString(2, doctor.id()); ps.setString(3, slot);
            ps.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Cannot save appointment: " + e.getMessage(), e); }
    }
}
