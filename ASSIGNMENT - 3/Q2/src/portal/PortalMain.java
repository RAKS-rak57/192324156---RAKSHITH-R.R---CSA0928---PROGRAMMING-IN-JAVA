package portal;
import java.time.LocalDateTime;
import java.util.*;

/** Student B: one scenario per exception, recorded in a test table. */
public class PortalMain {
    static final LocalDateTime OPEN = LocalDateTime.of(2026, 11, 10, 10, 0), CLOSES = LocalDateTime.of(2026, 11, 10, 12, 0);

    static ExamController newController(boolean lossy) {
        Map<String, Set<String>> reg = Map.of("CSA0928", Set.of("S001", "S002", "S003"));
        Map<String, LocalDateTime> win = Map.of("CSA0928", CLOSES);
        return new ExamController(new SubmissionService(reg, win, new Storage(), lossy));
    }
    static String chainOf(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable x = t; x != null; x = x.getCause()) {
            if (sb.length() > 0) sb.append("  <-caused by-  ");
            sb.append(x.getClass().getSimpleName());
        }
        return sb.toString();
    }
    public static void main(String[] args) {
        ExamController c = newController(false);
        String[][] rows = {
            // label, student, course, file, size, minutes after 10:00, expected exception
            {"valid submission", "S001", "CSA0928", "ans1.pdf", "120", "30", "none"},
            {"student not registered", "S999", "CSA0928", "ans2.pdf", "100", "30", "NotRegisteredException"},
            {"window closed (12:01)", "S002", "CSA0928", "ans3.pdf", "100", "121", "ExamWindowClosedException"},
            {"exactly at closing time (boundary)", "S002", "CSA0928", "ans3b.pdf", "50", "120", "none"},
            {"file too large (201 KB)", "S003", "CSA0928", "ans4.pdf", "201", "30", "FileTooLargeException"},
            {"file exactly at the limit (200 KB)", "S003", "CSA0928", "ans4b.pdf", "200", "30", "none"},
            {"disk quota exceeded (storage fails)", "S001", "CSA0928", "ans5.pdf", "190", "40", "SubmissionStorageException"},
            {"blank file name (programming error)", "S001", "CSA0928", " ", "10", "30", "InvalidSubmissionException"},
        };
        System.out.printf("| %-2s | %-36s | %-28s | %-60s | %-6s |%n", "#", "Scenario", "Expected exception", "Message the student sees", "Result");
        int n = 0, ok = 0;
        for (String[] r : rows) {
            n++;
            String msg = c.submit(r[1], r[2], r[3], Long.parseLong(r[4]), OPEN.plusMinutes(Long.parseLong(r[5])));
            String actual = c.lastFailure == null ? "none" : c.lastFailure.getClass().getSimpleName();
            boolean pass = actual.equals(r[6]);
            if (pass) ok++;
            System.out.printf("| %-2d | %-36s | %-28s | %-60s | %-6s |%n", n, r[0], actual, msg, pass ? "PASS" : "FAIL");
        }
        System.out.println();
        System.out.println("Result: " + ok + " / " + n + " scenarios behaved as expected");
        System.out.println();
        System.out.println("(chain for the storage failure is shown by ChainDemo)");
    }
}
