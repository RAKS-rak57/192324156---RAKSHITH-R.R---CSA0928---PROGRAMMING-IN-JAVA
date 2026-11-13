import java.nio.file.*;
import java.time.LocalDate;
import java.util.zip.CRC32;

/** Task B2 (second bullet): a damaged bus-pass file and the response of the file store (BusPassStore from Task B1). */
public class DamagedFileDemo {
    static String withCrc(String body) { CRC32 c = new CRC32(); c.update(body.getBytes()); return body + "," + Long.toHexString(c.getValue()); }
    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("damagedpasses", ".txt");
        BusPassStore store = new BusPassStore(f);
        store.append(new BusPass("P1001", "192311001", "Arun", 12, "Main Gate", LocalDate.of(2027, 6, 30), 4500));
        String good2 = new BusPass("P1002", "192311002", "Divya", 7, "Bus Stand", LocalDate.of(2027, 6, 30), 3800).toLine();
        String[][] bad = {
            {"wrong number of fields (3 instead of 8)", "P1100,192311100,Short"},
            {"too many fields (name with an unescaped comma)", withCrc("P1101,192311101,Kumar, Meena,12,Main Gate,2027-06-30,4500.00")},
            {"non-numeric amount (checksum correct)", withCrc("P1102,192311102,Ravi,3,Main Gate,2027-06-30,forty")},
            {"non-numeric route number", withCrc("P1103,192311103,Ravi,abc,Main Gate,2027-06-30,4500.00")},
            {"impossible validity date", withCrc("P1104,192311104,Ravi,3,Main Gate,2027-13-45,4500.00")},
            {"negative amount", withCrc("P1105,192311105,Ravi,3,Main Gate,2027-06-30,-50.00")},
            {"empty line", ""},
            {"line edited by hand (checksum mismatch)", good2.replace("Divya", "Divyaa")},
            {"line cut off by a power failure", good2.substring(0, good2.length() - 12)},
        };
        StringBuilder file = new StringBuilder();
        for (String[] b : bad) file.append(b[1]).append('\n');
        Files.writeString(f, file, StandardOpenOption.APPEND);
        store.append(new BusPass("P1003", "192311003", "Kavin", 12, "Main Gate", LocalDate.of(2027, 6, 30), 4500));

        System.out.println("=== The damaged file (" + Files.readAllLines(f).size() + " lines) ===");
        int i = 1; for (String l : Files.readAllLines(f)) System.out.printf("%2d | %s%n", i++, l.length() > 100 ? l.substring(0, 100) + "..." : l);
        System.out.println();
        System.out.println("=== Response of the store: list all passes of route 12 (a full scan) ===");
        System.out.println("passes found : " + store.listByRoute(12));
        System.out.println("damaged lines reported (" + store.damaged.size() + "):");
        for (String d : store.damaged) System.out.println("   " + d);
        System.out.println();
        System.out.println("what the damage reveals about the ORIGINAL design (5 fields, no checksum, String.split):");
        String original = "P1101,192311101,Kumar, Meena,12,4500.0";          // a name containing the delimiter, as the original PassStore would see it
        String[] parts = original.split(",", -1);
        System.out.println("   the original store would split  \"" + original + "\"  into " + parts.length + " fields -> silently ignored, the pass is lost");
        System.out.println("   with a non-numeric amount the original (no validation) would store it and fail later in Double.parseDouble");
        Files.delete(f);
    }
}
