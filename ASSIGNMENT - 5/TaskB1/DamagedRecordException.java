/** A line of the pass file could not be accepted (wrong field count, bad number, bad date, checksum mismatch ...). */
public class DamagedRecordException extends Exception {
    public DamagedRecordException(String message) { super(message); }
}
