package portal_unchecked;
/** Checked: the student is not registered for the course. */
public class NotRegisteredException extends ExamException {
    public NotRegisteredException(String student, String course) {
        super("Student " + student + " is not registered for " + course);
    }
}
