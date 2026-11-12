package campus.reg.model;

/** One line of the registrations table (already joined with student and event). */
public record RegistrationRow(int regId, String regNo, String studentName, String eventTitle, String registeredOn) { }
