package results;
/** Checked: the file exists but its CONTENT is not a valid score record. */
public class CorruptFileException extends Exception {
    public CorruptFileException(String message) { super(message); }
}
