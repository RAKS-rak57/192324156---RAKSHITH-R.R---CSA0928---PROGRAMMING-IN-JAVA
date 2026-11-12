import java.sql.SQLException;
import java.util.List;

/**
 * All the business rules of the fee window: validation, bank confirmation, saving, receipt text.
 * It has NO Swing / AWT code, so it can be tested with plain Java.
 */
public class FeeService {
    public static final double MIN_AMOUNT = 1.0;
    public static final double MAX_AMOUNT = 200000.0;

    private final PaymentRepository repository;
    private final BankGateway bank;

    public FeeService(PaymentRepository repository, BankGateway bank) {
        this.repository = repository;
        this.bank = bank;
    }

    /** Validates the two text values, asks the bank, saves the payment. Never throws: errors come back as a result. */
    public PaymentResult pay(String regNoText, String amountText) {
        String regNo = regNoText == null ? "" : regNoText.trim();
        String amt = amountText == null ? "" : amountText.trim();

        if (regNo.isEmpty())              return PaymentResult.fail("Register number is required.");
        if (!regNo.matches("\\d{9}"))     return PaymentResult.fail("Register number must be 9 digits (example 192311001).");
        if (amt.isEmpty())                return PaymentResult.fail("Amount is required.");

        double amount;
        try {
            amount = Double.parseDouble(amt);
        } catch (NumberFormatException e) {
            return PaymentResult.fail("Amount must be a number.");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            return PaymentResult.fail("Amount must be between " + (int) MIN_AMOUNT + " and " + (int) MAX_AMOUNT + ".");
        }

        try {
            if (!bank.confirm(regNo, amount)) return PaymentResult.fail("The bank did not confirm the payment.");
            long id = repository.save(regNo, amount);
            String message = String.format("Payment of Rs.%.2f received. Receipt RCPT-%05d.", amount, id);
            return PaymentResult.ok(message, receipt(regNo));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentResult.fail("Payment was interrupted.");
        } catch (SQLException e) {
            return PaymentResult.fail("Database error: " + e.getMessage());
        }
    }

    /** Text of all receipts of a student (used by the Receipt button). */
    public String receipt(String regNoText) {
        String regNo = regNoText == null ? "" : regNoText.trim();
        if (!regNo.matches("\\d{9}")) return "Enter a valid register number to see receipts.";
        try {
            List<String> lines = repository.history(regNo);
            if (lines.isEmpty()) return "No payments found for " + regNo + ".";
            StringBuilder sb = new StringBuilder("Receipts for " + regNo + "\n");
            for (String line : lines) sb.append(line).append('\n');
            return sb.toString().trim();
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }
}
