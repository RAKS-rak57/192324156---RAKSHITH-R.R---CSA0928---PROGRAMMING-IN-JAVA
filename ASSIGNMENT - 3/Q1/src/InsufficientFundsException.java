/** Checked: the customer asked for more than the balance. The caller MUST decide what to tell the customer. */
public class InsufficientFundsException extends Exception {
    private final int shortfall;
    public InsufficientFundsException(int balance, int amount) {
        super("Insufficient funds: balance " + balance + ", requested " + amount);
        this.shortfall = amount - balance;
    }
    public int getShortfall() { return shortfall; }
}
