package results;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Creates N score files (some corrupt, some missing) plus index.txt and expected_summary.txt (the ground truth). */
public class GenerateScores {
    public static void main(String[] args) throws IOException {
        int n = Integer.parseInt(args[0]);
        Path dir = Paths.get(args[1]);
        Files.createDirectories(dir);
        Random r = new Random(42);
        List<String> index = new ArrayList<>();
        int success = 0, corrupt = 0, missing = 0;
        for (int i = 1; i <= n; i++) {
            String name = String.format("score%05d.txt", i);
            index.add(name);
            int roll = r.nextInt(100);
            if (roll < 2) { missing++; continue; }                                  // listed in the index, but the file does not exist
            Path p = dir.resolve(name);
            if (roll < 5) {                                                         // 3 % corrupt, five different kinds
                corrupt++;
                switch (roll) {
                    case 2: Files.writeString(p, String.format("S%05d,70,abc,60,50,40", i)); break;
                    case 3: Files.writeString(p, String.format("S%05d,70,60,50,40,150", i)); break;
                    default: Files.writeString(p, ""); break;
                }
            } else {
                success++;
                Files.writeString(p, String.format("S%05d,%d,%d,%d,%d,%d", i, 30 + r.nextInt(71), 30 + r.nextInt(71), 30 + r.nextInt(71), 30 + r.nextInt(71), 30 + r.nextInt(71)));
            }
        }
        Files.write(dir.resolve("index.txt"), index);
        Files.writeString(dir.resolve("expected_summary.txt"), "submitted=" + n + " success=" + success + " corrupt=" + corrupt + " missing=" + missing + " error=0\n");
        System.out.println("generated " + n + " entries in " + dir + ": " + Files.readString(dir.resolve("expected_summary.txt")).trim());
    }
}
