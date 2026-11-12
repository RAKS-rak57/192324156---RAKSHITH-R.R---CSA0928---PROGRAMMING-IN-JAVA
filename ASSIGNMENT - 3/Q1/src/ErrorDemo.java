import java.io.FileNotFoundException;
import java.util.InputMismatchException;

/** Prints the part of the Throwable hierarchy used by the ATM, and shows that an Error is not an Exception. */
public class ErrorDemo {
    static int depth = 0;
    static void recurse() { depth++; recurse(); }

    static void chain(Class<?> c) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            sb.append(k.getSimpleName());
            if (k != Object.class) sb.append(" -> ");
        }
        System.out.println(sb);
    }
    public static void main(String[] args) {
        System.out.println("=== Superclass chains (child -> ... -> Object) ===");
        chain(InputMismatchException.class);
        chain(NumberFormatException.class);
        chain(IllegalArgumentException.class);
        chain(FileNotFoundException.class);
        chain(InsufficientFundsException.class);
        chain(AccountNotFoundException.class);
        chain(CardDataException.class);
        chain(StackOverflowError.class);
        chain(OutOfMemoryError.class);

        System.out.println();
        System.out.println("=== An Error is not an Exception ===");
        try {
            try { recurse(); }
            catch (Exception e) { System.out.println("caught by catch (Exception)  <- never printed"); }
        } catch (StackOverflowError e) {
            System.out.println("catch (Exception) did NOT catch it; only an Error handler did. Recursion depth reached = " + depth);
        }
        System.out.println("An Error means the JVM itself is in trouble; the ATM cannot repair it, so it is not handled (only logged at the top level).");
    }
}
