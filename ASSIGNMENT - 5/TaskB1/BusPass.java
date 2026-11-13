import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.zip.CRC32;

/**
 * One bus-pass record (Task A1).  Text line layout (8 comma-separated fields):
 *   passId , regNo , name , route , stop , validity , amount , crc32
 * A comma, a backslash or a line break INSIDE a field is written as  \,  \\  or  \n  (backslash escape).
 * The last field is the CRC-32 of the first seven fields, so a damaged or half-written line is detected.
 */
public class BusPass implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String passId, regNo, name, stop;
    public final int route;
    public final LocalDate validity;
    public final double amount;

    public BusPass(String passId, String regNo, String name, int route, String stop, LocalDate validity, double amount) {
        this.passId = passId; this.regNo = regNo; this.name = name; this.route = route;
        this.stop = stop; this.validity = validity; this.amount = amount;
    }

    // ---------- writing ----------
    static String escape(String s) { return s.replace("\\", "\\\\").replace(",", "\\,").replace("\n", "\\n").replace("\r", ""); }

    private String body() {
        return String.join(",", escape(passId), escape(regNo), escape(name), String.valueOf(route), escape(stop), validity.toString(), String.format("%.2f", amount));
    }

    public String toLine() {
        String b = body();
        CRC32 crc = new CRC32(); crc.update(b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return b + "," + Long.toHexString(crc.getValue());
    }

    // ---------- reading ----------
    /** Splits a line at the commas that are NOT escaped, and removes the escapes. */
    static String[] split(String line) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 1 < line.length()) {
                char next = line.charAt(++i);
                cur.append(next == 'n' ? '\n' : next);
            } else if (ch == ',') { parts.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    public static BusPass fromLine(String line) throws DamagedRecordException {
        if (line == null || line.isBlank()) throw new DamagedRecordException("empty line");
        String[] f = split(line);
        if (f.length != 8) throw new DamagedRecordException("expected 8 fields but found " + f.length);
        try {
            int lastComma = lastUnescapedComma(line);
            CRC32 crc = new CRC32(); crc.update(line.substring(0, lastComma).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (!Long.toHexString(crc.getValue()).equals(f[7])) throw new DamagedRecordException("checksum mismatch (line changed or cut off)");
            if (f[0].isBlank()) throw new DamagedRecordException("pass id is empty");
            return new BusPass(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4], LocalDate.parse(f[5]), parseAmount(f[6]));
        } catch (NumberFormatException e) { throw new DamagedRecordException("not a number: " + e.getMessage()); }
        catch (DateTimeParseException e) { throw new DamagedRecordException("bad validity date '" + f[5] + "'"); }
    }

    private static double parseAmount(String s) throws DamagedRecordException {
        double a = Double.parseDouble(s);
        if (Double.isNaN(a) || a <= 0) throw new DamagedRecordException("amount must be positive: " + s);
        return a;
    }

    private static int lastUnescapedComma(String line) {
        int last = -1;
        for (int i = 0; i < line.length(); i++) { char ch = line.charAt(i); if (ch == '\\') i++; else if (ch == ',') last = i; }
        return last;
    }

    @Override public String toString() { return passId + " " + regNo + " " + name + " route " + route + " " + stop + " valid " + validity + " Rs." + amount; }
}
