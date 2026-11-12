package campus.reg.dao;

/** The student is already registered for the event (business rule, not a crash). */
public class DuplicateRegistrationException extends Exception {
    public DuplicateRegistrationException(String message) { super(message); }
}
