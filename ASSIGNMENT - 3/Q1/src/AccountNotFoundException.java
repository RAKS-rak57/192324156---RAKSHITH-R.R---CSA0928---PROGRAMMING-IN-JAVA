/** Checked: the account number is not in the card-reader file. */
public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(int account) { super("Account not found: " + account); }
}
