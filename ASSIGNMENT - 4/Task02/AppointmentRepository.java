import java.util.List;
import java.util.Map;

public interface AppointmentRepository {
    /** One query for everything: used once at start-up (cache approach). */
    Map<String, List<Doctor>> loadAllDoctors();
    /** One query per call: used when the doctors are fetched on every department change. */
    List<Doctor> findDoctors(String department);
    List<String> slots();
    boolean isSlotTaken(String doctorId, String slot);
    void save(String patientId, Doctor doctor, String slot);
}
