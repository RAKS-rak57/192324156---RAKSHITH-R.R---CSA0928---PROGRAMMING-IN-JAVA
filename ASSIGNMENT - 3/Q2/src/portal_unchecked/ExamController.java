package portal_unchecked;
import java.time.LocalDateTime;
/** CONTROLLER layer: the last place an exception can be handled. Translates every failure into a message for the student. */
public class ExamController {
    private final SubmissionService service;
    public Throwable lastFailure;                          // kept so that tests can inspect the exception that reached here
    public ExamController(SubmissionService service) { this.service = service; }

    public String submit(String student, String course, String file, long sizeKb, LocalDateTime now) {
        lastFailure = null;
        try {
            service.submit(student, course, file, sizeKb, now);
            return "OK: " + file + " submitted";
        } catch (ExamWindowClosedException e) {
            lastFailure = e; return "REJECTED: the exam window has closed";
        } catch (NotRegisteredException e) {
            lastFailure = e; return "REJECTED: you are not registered for " + course;
        } catch (FileTooLargeException e) {
            lastFailure = e; return "REJECTED: your file is larger than " + SubmissionService.SIZE_LIMIT_KB + " KB - please compress it";
        } catch (SubmissionStorageException e) {
            lastFailure = e; return "ERROR: we could not save your answer, please try again (reference logged)";
        } catch (ExamException e) {                         // any future business exception
            lastFailure = e; return "ERROR: " + e.getMessage();
        } catch (InvalidSubmissionException e) {            // unchecked programming error: reported, not hidden
            lastFailure = e; return "ERROR: invalid request";
        }
    }
}
