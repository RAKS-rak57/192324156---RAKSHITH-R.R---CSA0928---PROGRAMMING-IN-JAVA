/** The (slow) bank confirmation, hidden behind an interface so tests can use an instant one. */
public interface BankGateway {
    boolean confirm(String regNo, double amount) throws InterruptedException;
}
