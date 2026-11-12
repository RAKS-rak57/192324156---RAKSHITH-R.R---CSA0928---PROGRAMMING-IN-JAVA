package campus.reg.dao;

/** Unexpected database problem (connection lost, bad SQL ...). The user interface shows a short message. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
