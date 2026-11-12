import java.util.*;

/** Stand-in for the database (used for tests / demo). latencyMillis imitates one network round trip per query. */
public class InMemoryAppointmentRepository implements AppointmentRepository {
    private final List<Doctor> doctors = new ArrayList<>();
    private final List<String> slots = List.of("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30");
    private final Set<String> booked = new HashSet<>();           // doctorId + "|" + slot
    private final long latencyMillis;

    public InMemoryAppointmentRepository() { this(0); seedDemoData(); }
    public InMemoryAppointmentRepository(long latencyMillis) { this.latencyMillis = latencyMillis; }

    public void addDoctor(Doctor d) { doctors.add(d); }

    private void seedDemoData() {
        String[][] data = {
            {"Cardiology", "Dr. Anand Rao", "Dr. Meera Nair", "Dr. S. Kumar"},
            {"Orthopaedics", "Dr. Rajesh Iyer", "Dr. Latha Menon", "Dr. A. Fernandes"},
            {"Paediatrics", "Dr. Kavitha Rao", "Dr. Imran Khan", "Dr. Sneha Pillai"},
            {"Dermatology", "Dr. Priya Sharma", "Dr. V. Balaji", "Dr. Nisha George"}};
        int n = 1;
        for (String[] row : data)
            for (int i = 1; i < row.length; i++) doctors.add(new Doctor("D" + (n++), row[i], row[0]));
    }

    private void pause() {
        if (latencyMillis > 0) try { Thread.sleep(latencyMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Override public Map<String, List<Doctor>> loadAllDoctors() {
        pause();
        Map<String, List<Doctor>> map = new TreeMap<>();
        for (Doctor d : doctors) map.computeIfAbsent(d.department(), k -> new ArrayList<>()).add(d);
        return map;
    }

    @Override public List<Doctor> findDoctors(String department) {
        pause();
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : doctors) if (d.department().equals(department)) result.add(d);   // full scan, like a query with no index
        return result;
    }

    @Override public List<String> slots() { return slots; }
    @Override public boolean isSlotTaken(String doctorId, String slot) { return booked.contains(doctorId + "|" + slot); }
    @Override public void save(String patientId, Doctor doctor, String slot) { booked.add(doctor.id() + "|" + slot); }
}
