import java.util.*;

public class MigrationReport {
    public int linesRead, loaded, rejected, duplicates;
    public long millis;
    public final Map<String, Integer> reasons = new TreeMap<>();
    public final List<String> rejectLines = new ArrayList<>();
    public void reject(int lineNo, String reason, String raw) {
        rejected++;
        String key = reason.replaceAll("[0-9.\\-]+", "#").replaceAll("'[^']*'", "'..'").replaceAll("\"[^\"]*\"", "\"..\"");
        reasons.merge(key, 1, Integer::sum);
        rejectLines.add("line " + lineNo + ": " + reason + "   | " + (raw.length() > 70 ? raw.substring(0, 70) + "..." : raw));
    }
}
