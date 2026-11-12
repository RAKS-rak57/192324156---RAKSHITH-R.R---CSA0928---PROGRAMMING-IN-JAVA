package results;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Student B: inject failures INSIDE the tasks (not just bad files) and check that the summary is still complete and correct. */
public class FaultInjectionTest {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get("input/handset");
        List<Path> files = Main.readIndex(dir);
        Set<String> boom = Set.of("f03.txt", "f08.txt");            // f03 is a good file, f08 a corrupt one
        ResultProcessor.TaskFactory faulty = (file, sink, latency) -> {
            String name = file.getFileName().toString();
            if (name.equals("f03.txt")) return () -> { throw new IllegalStateException("injected bug in task for " + name); };
            if (name.equals("f08.txt")) return () -> { throw new AssertionError("injected Error in task for " + name); };    // an Error, not an Exception
            return new ScoreFileTask(file, sink, latency);
        };
        ResultProcessor.Run run = ResultProcessor.process(files, 4, 0, faulty);
        run.summary().print(10);
        System.out.println();
        System.out.println("expected: 15 submitted; f03 (was SUCCESS) and f08 (was CORRUPT) now ERROR -> success=6 corrupt=4 missing=2 error=3 (1 directory + 2 injected)");
        System.out.println("actual  : " + run.summary().counts());
        boolean ok = run.summary().complete && run.summary().success == 6 && run.summary().corrupt == 4 && run.summary().missing == 2 && run.summary().error == 3;
        System.out.println(ok ? "PASS - the run was not stopped, every file is accounted for, and the two injected failures are reported" : "FAIL");
    }
}
