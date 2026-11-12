package portal;
import java.time.LocalDateTime;
/** Checked: the student tried to submit after the deadline. Expected business event, student must be told. */
public class ExamWindowClosedException extends ExamException {
    public ExamWindowClosedException(LocalDateTime closedAt, LocalDateTime now) {
        super("Exam window closed at " + closedAt + " (submission attempted at " + now + ")");
    }
}
