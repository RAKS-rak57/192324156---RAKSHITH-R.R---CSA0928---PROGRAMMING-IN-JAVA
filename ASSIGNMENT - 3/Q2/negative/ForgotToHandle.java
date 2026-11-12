import portal.*;
import java.time.LocalDateTime;
public class ForgotToHandle {
    // With CHECKED exceptions the compiler refuses code that ignores a failure.
    static void careless(SubmissionService s) {
        s.submit("S001", "CSA0928", "a.pdf", 10, LocalDateTime.now());
    }
}
