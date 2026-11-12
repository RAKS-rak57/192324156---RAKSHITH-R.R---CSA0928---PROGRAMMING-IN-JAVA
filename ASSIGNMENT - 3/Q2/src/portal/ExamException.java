package portal;
/** Checked root of every business failure of the portal. Callers are FORCED to decide what to do. */
public class ExamException extends Exception {
    public ExamException(String message) { super(message); }
    public ExamException(String message, Throwable cause) { super(message, cause); }
}
