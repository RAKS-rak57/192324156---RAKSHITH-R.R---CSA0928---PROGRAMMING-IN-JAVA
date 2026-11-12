package portal_unchecked;
/** Checked root of every business failure of the portal. Callers are FORCED to decide what to do. */
public class ExamException extends RuntimeException {
    public ExamException(String message) { super(message); }
    public ExamException(String message, Throwable cause) { super(message, cause); }
}
