import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Q1 - ATM withdrawal with try / catch (incl. multi-catch) / finally. */
public class Atm {
    static int finallyRuns = 0;                       // how many times a finally block has executed

    /** Returns the PATH taken (SUCCESS or the name of the failure) so that tests can check it. */
    static String withdraw(String request, String cardFile) {
        BufferedReader reader = null;
        String path = "SUCCESS";
        try {
            Scanner in = new Scanner(request);
            int account = in.nextInt();               // letters      -> InputMismatchException (unchecked)
            int amount = in.nextInt();                // missing token-> NoSuchElementException (unchecked)
            if (amount <= 0)                          // negative / zero -> our own unchecked failure
                throw new IllegalArgumentException("Amount must be positive: " + amount);
            reader = new BufferedReader(new FileReader(cardFile));      // missing file -> FileNotFoundException (checked)
            int balance = findBalance(reader, account);                 // IOException, AccountNotFound, CardData (checked)
            if (amount > balance) throw new InsufficientFundsException(balance, amount);
            System.out.println("   dispensed " + amount + ", new balance " + (balance - amount));

        } catch (InputMismatchException | IllegalArgumentException e) {   // MULTI-CATCH: two unrelated types, same handling
            path = "BAD_INPUT";
            System.out.println("   Please enter a valid account number and a positive amount (" + e.getClass().getSimpleName() + ")");
        } catch (NoSuchElementException e) {          // superclass of InputMismatchException: must come AFTER it
            path = "MISSING_INPUT";
            System.out.println("   Please enter both the account number and the amount");
        } catch (InsufficientFundsException e) {
            path = "INSUFFICIENT_FUNDS";
            System.out.println("   " + e.getMessage() + " (short by " + e.getShortfall() + ")");
        } catch (AccountNotFoundException e) {
            path = "ACCOUNT_NOT_FOUND";
            System.out.println("   " + e.getMessage());
        } catch (CardDataException e) {
            path = "CARD_DATA";
            System.out.println("   Card data damaged: " + e.getMessage() + "  [cause: " + e.getCause().getClass().getSimpleName() + "]");
        } catch (FileNotFoundException e) {           // subclass of IOException: must come BEFORE it
            path = "CARD_FILE_MISSING";
            System.out.println("   Card reader unavailable: " + cardFile);
        } catch (IOException e) {
            path = "IO_ERROR";
            System.out.println("   Card reader error: " + e.getMessage());
        } catch (Exception e) {                       // last-resort safety net, AFTER every specific handler
            path = "UNEXPECTED";
            System.out.println("   Unexpected problem, transaction cancelled: " + e);
        } finally {
            finallyRuns++;                            // runs on EVERY path: success, handled failure, even return
            System.out.println("   [finally] card reader released, session closed");
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) { }
            }
        }
        return path;
    }

    private static int findBalance(BufferedReader r, int account)
            throws IOException, AccountNotFoundException, CardDataException {
        String line;
        while ((line = r.readLine()) != null) {
            String[] p = line.split(",");
            try {
                if (Integer.parseInt(p[0].trim()) == account) return Integer.parseInt(p[1].trim());
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new CardDataException("bad line \"" + line + "\"", e);   // wrap: file damage is not the user's mistake
            }
        }
        throw new AccountNotFoundException(account);
    }
}
