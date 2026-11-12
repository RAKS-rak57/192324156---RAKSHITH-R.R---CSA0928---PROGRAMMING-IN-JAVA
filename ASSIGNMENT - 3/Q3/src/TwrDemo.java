import java.util.Arrays;

/** Student B: closing order, suppressed exceptions, and how manual finally can hide the original failure. */
public class TwrDemo {
    static class Res implements AutoCloseable {
        final String name; final boolean failOnClose;
        Res(String name, boolean failOnClose) { this.name = name; this.failOnClose = failOnClose; System.out.println("  open  " + name); }
        @Override public void close() {
            System.out.println("  close " + name);
            if (failOnClose) throw new IllegalStateException("close() of " + name + " failed");
        }
    }
    public static void main(String[] args) {
        System.out.println("=== 1. Closing order: reverse of opening ===");
        try (Res a = new Res("A (opened first)", false); Res b = new Res("B (opened second)", false)) {
            System.out.println("  body runs");
        }

        System.out.println();
        System.out.println("=== 2. Body fails AND close() fails -> the close failure is SUPPRESSED, the body failure is reported ===");
        try (Res a = new Res("A", true); Res b = new Res("B", true)) {
            throw new RuntimeException("body failed (the PRIMARY exception)");
        } catch (RuntimeException e) {
            System.out.println("  caught   : " + e.getMessage());
            System.out.println("  suppressed (" + e.getSuppressed().length + "): " + Arrays.toString(e.getSuppressed()));
        }

        System.out.println();
        System.out.println("=== 3. The same situation with manual finally -> the ORIGINAL failure is lost ===");
        Res a = null, b = null;
        try {
            try {
                a = new Res("A", true); b = new Res("B", true);
                throw new RuntimeException("body failed (the PRIMARY exception)");
            } finally {
                try { if (b != null) b.close(); } finally { if (a != null) a.close(); }   // manual close, no suppression
            }
        } catch (RuntimeException e) {
            System.out.println("  caught   : " + e.getMessage() + "   <- the body failure and B's close failure are GONE");
            System.out.println("  suppressed (" + e.getSuppressed().length + "): nothing recorded");
        }
    }
}
