package portal_unchecked;
/** Checked: the storage layer failed. Wraps the low-level cause (IOException) so it is never lost. */
public class SubmissionStorageException extends ExamException {
    public SubmissionStorageException(String message, Throwable cause) { super(message, cause); }
    public SubmissionStorageException(String message) { super(message); }          // used ONLY by the "lossy" demo
}
