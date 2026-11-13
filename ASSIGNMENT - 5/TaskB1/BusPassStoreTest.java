import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

/** Task B1.1 - tests of the store: normal cases, an id that does not exist, and damaged data. */
public class BusPassStoreTest {
    static int pass = 0, fail = 0;
    static void check(String id, String what, boolean ok, String detail) {
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-4s %-52s %s%n", ok ? "PASS" : "FAIL", id, what, detail);
    }
    static BusPass p(String id, String reg, String name, int route) { return new BusPass(id, reg, name, route, "Main Gate", LocalDate.of(2027, 6, 30), 4500); }

    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("buspass", ".txt");
        Files.delete(f);                                                       // start with a MISSING file
        BusPassStore store = new BusPassStore(f);

        System.out.println("=== B1.1 Test table (what each test verifies is in the text of the report) ===");
        check("T0", "find() on a missing file", store.find("P1001") == null, "returns null, no exception: a missing file = empty store");
        store.add(p("P1001", "192311001", "Arun", 12));
        store.add(p("P1002", "192311002", "Divya", 7));
        store.add(p("P1003", "192311003", "Kumar, Meena", 12));                 // name contains the delimiter
        store.add(p("P1004", "192311004", "A\\B", 9));                          // name contains the escape character
        BusPass x = store.find("P1002");
        check("T1", "NORMAL: add 4 passes, find P1002", x != null && x.name.equals("Divya") && x.route == 7, String.valueOf(x));
        x = store.find("P1003");
        check("T2", "NORMAL: name with a comma survives", x != null && x.name.equals("Kumar, Meena"), "stored line: " + Files.readAllLines(f).get(2));
        x = store.find("P1004");
        check("T3", "NORMAL: name with a backslash survives", x != null && x.name.equals("A\\B"), String.valueOf(x));
        List<BusPass> r = store.listByRoute(12);
        check("T4", "NORMAL: list route 12 gives 2 passes", r.size() == 2, r.get(0).passId + ", " + r.get(1).passId);
        check("T5", "NOT FOUND: find(\"P9999\") returns null", store.find("P9999") == null, "no exception");
        boolean dup = false; try { store.add(p("P1001", "192311009", "Other", 1)); } catch (IOException e) { dup = true; }
        check("T6", "duplicate pass id is rejected", dup, "IOException: pass id already exists");

        System.out.println();
        System.out.println("=== damaged data: five bad lines are put between good ones ===");
        String good = p("P2000", "192312000", "Good", 5).toLine();
        String badAmountBody = "P1501,192311501,Ravi,3,Gate,2027-06-30,abc";
        java.util.zip.CRC32 crc = new java.util.zip.CRC32(); crc.update(badAmountBody.getBytes());
        Files.writeString(f, "P1500,192311500,Short\n"                                    // wrong number of fields
                + badAmountBody + "," + Long.toHexString(crc.getValue()) + "\n"           // non-numeric amount (checksum is correct)
                + "\n"                                                                    // empty line
                + good.replace("Good", "Goop") + "\n"                                     // changed text: checksum no longer matches
                + good + "\n"                                                             // a good line after the damage
                + good.substring(0, good.length() - 7),                                   // last line cut off (no line break): power failure
                StandardOpenOption.APPEND);
        x = store.find("P2000");
        check("T7", "DAMAGED lines do not stop the search", x != null && x.name.equals("Good"), "P2000 found after 5 bad lines");
        System.out.println("     damaged lines reported by the scan:");
        for (String d : store.damaged) System.out.println("       " + d);
        System.out.println("     (find() stops at P2000, so the cut-off last line was not reached yet; now a FULL scan:)");
        store.listByRoute(5);
        for (String d : store.damaged) System.out.println("       " + d);
        check("T8", "full scan reports exactly the 5 damaged lines", store.damaged.size() == 5 && store.damaged.get(4).startsWith("line 10"), store.damaged.size() + " reported, the last one is the cut-off line 10");
        check("T9", "good records before the damage are still found", store.find("P1001") != null, "P1001 found");

        store.add(p("P3000", "192313000", "After", 1));                                // add after a cut-off last line
        check("T10", "add after a cut-off last line does not glue records", store.find("P3000") != null, "newline repaired before appending");

        System.out.println();
        System.out.println("Result: " + pass + " passed, " + fail + " failed");
        Files.delete(f);
    }
}
