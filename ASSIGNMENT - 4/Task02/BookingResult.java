public class BookingResult {
    public final boolean success;
    public final String message;
    private BookingResult(boolean s, String m) { success = s; message = m; }
    public static BookingResult ok(String m) { return new BookingResult(true, m); }
    public static BookingResult fail(String m) { return new BookingResult(false, m); }
}
