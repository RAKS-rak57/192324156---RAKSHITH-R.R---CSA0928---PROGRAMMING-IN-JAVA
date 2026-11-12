package portal;
/** Checked: the answer file exceeds the size limit; the student can fix it and resubmit. */
public class FileTooLargeException extends ExamException {
    public FileTooLargeException(String fileName, long sizeKb, long limitKb) {
        super("File " + fileName + " is " + sizeKb + " KB; the limit is " + limitKb + " KB");
    }
}
