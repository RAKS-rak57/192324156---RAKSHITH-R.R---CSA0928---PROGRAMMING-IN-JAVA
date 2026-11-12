package portal;
import java.time.LocalDateTime;
import java.util.*;

/** Student B: exception chaining with getCause(), the full chained stack trace, and what is lost without the cause. */
public class ChainDemo {
    static SubmissionService service(boolean lossy) {
        return new SubmissionService(Map.of("CSA0928", Set.of("S001")), Map.of("CSA0928", LocalDateTime.of(2026, 11, 10, 12, 0)), new Storage(), lossy);
    }
    static void walk(Throwable t) {
        int level = 0;
        for (Throwable x = t; x != null; x = x.getCause(), level++)
            System.out.println("  level " + level + ": " + x.getClass().getName() + " : " + x.getMessage());
    }
    static void tryTwice(SubmissionService s, String label) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 10, 10, 30);
        try {
            s.submit("S001", "CSA0928", "ok.pdf", 190, now);
            s.submit("S001", "CSA0928", "big.pdf", 190, now);     // second file exceeds the 500 KB total quota? 190+190 = 380 (ok)
            s.submit("S001", "CSA0928", "third.pdf", 190, now);    // 570 > 500 -> storage failure
        } catch (ExamException e) {
            System.out.println("--- " + label + " ---");
            System.out.println("getMessage() = " + e.getMessage());
            System.out.println("getCause()   = " + e.getCause());
            walk(e);
            System.out.println("full stack trace:");
            e.printStackTrace(System.out);
        }
    }
    public static void main(String[] args) {
        tryTwice(service(false), "WITH chaining (cause kept)");
        System.out.println();
        tryTwice(service(true), "WITHOUT the cause (re-thrown as a new exception)");
    }
}
