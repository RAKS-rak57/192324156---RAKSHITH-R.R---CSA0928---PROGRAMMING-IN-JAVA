import java.util.*;

/** Rules of the appointment desk. The doctors of every department are loaded ONCE into a Map and reused. */
public class AppointmentService {
    public static final String PROMPT = "-- select --";
    private final AppointmentRepository repository;
    private final Map<String, List<Doctor>> doctorsByDepartment;

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
        this.doctorsByDepartment = new TreeMap<>(repository.loadAllDoctors());     // one database access
    }

    public List<String> departments() { return new ArrayList<>(doctorsByDepartment.keySet()); }
    public List<String> slots() { return repository.slots(); }

    /** O(1) lookup in the cached Map (no database access). */
    public List<Doctor> doctorsFor(String department) {
        return doctorsByDepartment.getOrDefault(department, Collections.emptyList());
    }

    public BookingResult book(String patientId, String department, Doctor doctor, String slot) {
        String id = patientId == null ? "" : patientId.trim();
        if (id.isEmpty())                                   return BookingResult.fail("Enter the patient ID.");
        if (!id.matches("P\\d{4}"))                         return BookingResult.fail("Patient ID must look like P1001.");
        if (department == null || department.equals(PROMPT)) return BookingResult.fail("Select a department.");
        if (doctor == null)                                 return BookingResult.fail("Select a doctor.");
        if (!doctor.department().equals(department))       return BookingResult.fail("The doctor does not belong to " + department + ".");
        if (slot == null || slot.equals(PROMPT))            return BookingResult.fail("Select a time slot.");
        if (repository.isSlotTaken(doctor.id(), slot))      return BookingResult.fail(doctor + " is already booked at " + slot + ".");
        repository.save(id, doctor, slot);
        return BookingResult.ok("Appointment confirmed: " + id + " with " + doctor + " (" + department + ") at " + slot + ".");
    }
}
