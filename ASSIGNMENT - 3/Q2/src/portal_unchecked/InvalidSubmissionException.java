package portal_unchecked;
/** UNCHECKED: a programming error (null / blank argument). The caller could have avoided it; nobody can "recover" from it. */
public class InvalidSubmissionException extends RuntimeException {
    public InvalidSubmissionException(String message) { super(message); }
}
