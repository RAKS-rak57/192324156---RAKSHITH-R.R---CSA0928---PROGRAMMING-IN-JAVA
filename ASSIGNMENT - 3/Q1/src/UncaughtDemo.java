/** Student B: an exception that nobody catches - read the stack trace from the top. */
public class UncaughtDemo {
    public static void main(String[] args) {
        System.out.println("ATM started");
        process("abc");
        System.out.println("this line is never printed");
    }
    static void process(String amountText) {
        int amount = parseAmount(amountText);
        System.out.println("amount = " + amount);
    }
    static int parseAmount(String text) {
        return Integer.parseInt(text);
    }
}
