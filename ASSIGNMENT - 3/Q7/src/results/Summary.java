package results;

import java.util.*;
import java.util.stream.Collectors;

/** The end-of-run summary, built from the shared result map ONLY after every task has finished. */
public class Summary {
    public final int submitted, success, corrupt, missing, error;
    public final Map<String, Long> grades; public final double meanAverage; public final List<String> failures;
    public final boolean complete;

    public Summary(int submitted, Collection<FileResult> all) {
        this.submitted = submitted;
        Map<Status, Long> c = all.stream().collect(Collectors.groupingBy(FileResult::status, Collectors.counting()));
        success = c.getOrDefault(Status.SUCCESS, 0L).intValue(); corrupt = c.getOrDefault(Status.CORRUPT, 0L).intValue();
        missing = c.getOrDefault(Status.MISSING, 0L).intValue(); error = c.getOrDefault(Status.ERROR, 0L).intValue();
        grades = all.stream().filter(r -> r.status() == Status.SUCCESS).collect(Collectors.groupingBy(FileResult::grade, TreeMap::new, Collectors.counting()));
        meanAverage = all.stream().filter(r -> r.status() == Status.SUCCESS).mapToDouble(FileResult::average).average().orElse(0);
        failures = all.stream().filter(r -> r.status() != Status.SUCCESS).sorted(Comparator.comparing(FileResult::fileName))
                .map(r -> r.fileName() + " [" + r.status() + "] " + r.message()).collect(Collectors.toList());
        complete = (success + corrupt + missing + error) == submitted;      // every file accounted for exactly once
    }
    public String counts() { return "submitted=" + submitted + " success=" + success + " corrupt=" + corrupt + " missing=" + missing + " error=" + error; }
    public void print(int maxFailures) {
        System.out.println("SUMMARY  " + counts() + "   complete=" + complete);
        System.out.println("grades   " + grades + "   mean average of successful files = " + String.format(Locale.ROOT, "%.2f", meanAverage));
        System.out.println("failures (" + failures.size() + "):");
        for (int i = 0; i < Math.min(maxFailures, failures.size()); i++) System.out.println("   " + failures.get(i));
        if (failures.size() > maxFailures) System.out.println("   ... " + (failures.size() - maxFailures) + " more");
    }
}
