package results;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** java results.Main <dir> <threads> [latencyMicros]   - processes every file listed in <dir>/index.txt */
public class Main {
    public static List<Path> readIndex(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String line : Files.readAllLines(dir.resolve("index.txt"))) if (!line.isBlank()) files.add(dir.resolve(line.trim()));
        return files;
    }
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]); int threads = Integer.parseInt(args[1]); long latency = args.length > 2 ? Long.parseLong(args[2]) : 0;
        ResultProcessor.Run run = ResultProcessor.process(readIndex(dir), threads, latency);
        run.summary().print(10);
        System.out.printf("time = %.0f ms with %d thread(s), simulated latency %d us%n", run.millis(), threads, latency);
        Path expected = dir.resolve("expected_summary.txt");
        if (Files.exists(expected)) {
            String e = Files.readString(expected).trim();
            System.out.println("expected  " + e);
            System.out.println("actual    " + run.summary().counts());
            System.out.println(e.equals(run.summary().counts()) ? "MATCH" : "MISMATCH");
        }
    }
}
