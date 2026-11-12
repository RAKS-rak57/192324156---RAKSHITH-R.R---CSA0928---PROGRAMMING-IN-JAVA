package results;

/** Validates one score record:  S12345,m1,m2,m3,m4,m5   (five marks, each an integer 0..100). */
public final class ScoreParser {
    private ScoreParser() { }
    public record Parsed(String id, int total, double average, String grade) { }

    public static Parsed parse(String content) throws CorruptFileException {
        if (content == null || content.isBlank()) throw new CorruptFileException("empty file");
        String[] f = content.trim().split(",");
        if (f.length != 6) throw new CorruptFileException("expected 6 fields, found " + f.length);
        String id = f[0].trim();
        if (!id.matches("S\\d{5}")) throw new CorruptFileException("bad student id '" + id + "'");
        int total = 0; boolean anyBelow35 = false;
        for (int i = 1; i < 6; i++) {
            int m;
            try { m = Integer.parseInt(f[i].trim()); }
            catch (NumberFormatException e) { throw new CorruptFileException("mark " + i + " is not a number: '" + f[i].trim() + "'"); }
            if (m < 0 || m > 100) throw new CorruptFileException("mark " + i + " out of range 0..100: " + m);
            if (m < 35) anyBelow35 = true;
            total += m;
        }
        double avg = total / 5.0;
        String grade = anyBelow35 ? "FAIL" : avg >= 75 ? "A" : avg >= 60 ? "B" : avg >= 50 ? "C" : avg >= 40 ? "D" : "FAIL";
        return new Parsed(id, total, avg, grade);
    }
}
