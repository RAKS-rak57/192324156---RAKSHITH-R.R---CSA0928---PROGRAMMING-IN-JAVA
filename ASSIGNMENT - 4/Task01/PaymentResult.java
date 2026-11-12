/** What the service tells the window after a payment attempt. The window only displays it. */
public class PaymentResult {
    public final boolean success;
    public final String message;
    public final String receiptText;      // empty when the payment failed

    private PaymentResult(boolean success, String message, String receiptText) {
        this.success = success;
        this.message = message;
        this.receiptText = receiptText;
    }
    public static PaymentResult ok(String message, String receiptText) { return new PaymentResult(true, message, receiptText); }
    public static PaymentResult fail(String message) { return new PaymentResult(false, message, ""); }
}
