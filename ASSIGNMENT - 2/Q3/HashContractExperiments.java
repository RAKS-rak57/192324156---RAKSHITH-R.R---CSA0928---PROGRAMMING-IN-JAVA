import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Q3 - Investigating the equals()/hashCode() contract.
 * Four tests, each with a prediction printed BEFORE the result.
 */
public class HashContractExperiments {

    // junior developer's class: equals() only
    static class BadStudent {
        String regNo; String name;
        BadStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof BadStudent)) return false;
            return regNo.equals(((BadStudent) o).regNo);
        }
        @Override public String toString() { return regNo + " " + name; }
    }

    // corrected class: equals() and hashCode() both use regNo (regNo is NOT final here, for Test 3)
    static class GoodStudent {
        String regNo; String name;
        GoodStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoodStudent)) return false;
            return regNo.equals(((GoodStudent) o).regNo);
        }
        @Override public int hashCode() { return Objects.hash(regNo); }
        @Override public String toString() { return regNo + " " + name; }
    }

    // same bucket index formula that java.util.HashMap uses for a table of size 16
    static int bucket(int hash) {
        int h = hash ^ (hash >>> 16);
        return (16 - 1) & h;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Q3 : hashCode - equals contract experiments ===");

        // ---------------- TEST 1 ----------------
        System.out.println();
        System.out.println("TEST 1 : every student scanned once, one card object scanned twice (same object)");
        System.out.println("PREDICTION: size 3 for BadStudent and GoodStudent. Distinct students have different regNo, and the");
        System.out.println("            repeated scan is the SAME object, so even the default identity hashCode is equal.");
        Set<BadStudent> bad1 = new HashSet<>();
        BadStudent arun = new BadStudent("192311001", "Arun");
        bad1.add(arun);
        bad1.add(new BadStudent("192311002", "Divya"));
        bad1.add(new BadStudent("192311003", "Kavin"));
        bad1.add(arun);                                       // same object again
        Set<GoodStudent> good1 = new HashSet<>();
        GoodStudent arun2 = new GoodStudent("192311001", "Arun");
        good1.add(arun2);
        good1.add(new GoodStudent("192311002", "Divya"));
        good1.add(new GoodStudent("192311003", "Kavin"));
        good1.add(arun2);
        System.out.println("OBSERVED  : BadStudent set size = " + bad1.size() + ", GoodStudent set size = " + good1.size());

        // ---------------- TEST 2 ----------------
        System.out.println();
        System.out.println("TEST 2 : the same student scanned again at another gate (a NEW object each time)");
        System.out.println("PREDICTION: BadStudent -> 3 (duplicate appears: equals says equal but hashCodes differ, so the");
        System.out.println("            second object goes to a different bucket and equals() is never called).");
        System.out.println("            GoodStudent -> 2.");
        Set<BadStudent> bad2 = new HashSet<>();
        Set<GoodStudent> good2 = new HashSet<>();
        bad2.add(new BadStudent("192311001", "Arun"));
        bad2.add(new BadStudent("192311002", "Divya"));
        bad2.add(new BadStudent("192311001", "Arun"));
        good2.add(new GoodStudent("192311001", "Arun"));
        good2.add(new GoodStudent("192311002", "Divya"));
        good2.add(new GoodStudent("192311001", "Arun"));
        System.out.println("OBSERVED  : BadStudent set size = " + bad2.size() + ", GoodStudent set size = " + good2.size());
        System.out.println("            Bad set contents : " + bad2);
        BadStudent x = new BadStudent("192311001", "Arun"), y = new BadStudent("192311001", "Arun");
        System.out.println("            x.equals(y) = " + x.equals(y) + "   x.hashCode() = " + x.hashCode()
                + " (bucket " + bucket(x.hashCode()) + ")   y.hashCode() = " + y.hashCode() + " (bucket " + bucket(y.hashCode()) + ")");
        GoodStudent gx = new GoodStudent("192311001", "Arun"), gy = new GoodStudent("192311001", "Arun");
        System.out.println("            good: hashCode " + gx.hashCode() + " and " + gy.hashCode() + " -> same bucket " + bucket(gx.hashCode()));

        // bigger version: 100 students, each scanned 10 times
        Set<BadStudent> bigBad = new HashSet<>();
        Set<GoodStudent> bigGood = new HashSet<>();
        for (int round = 0; round < 10; round++) {
            for (int i = 0; i < 100; i++) {
                bigBad.add(new BadStudent("REG" + i, "S" + i));
                bigGood.add(new GoodStudent("REG" + i, "S" + i));
            }
        }
        System.out.println("            Bigger run (100 students x 10 scans = 1000 scans): Bad size = " + bigBad.size()
                + ", Good size = " + bigGood.size() + "  (correct answer is 100)");

        // ---------------- TEST 3a ----------------
        System.out.println();
        System.out.println("TEST 3a : change regNo AFTER the student is stored in the HashSet (GoodStudent, regNo not final)");
        System.out.println("PREDICTION: contains(...) fails for both old and new regNo, and adding the new regNo gives size 2,");
        System.out.println("            because the object sits in the bucket of the OLD hash but now has the NEW regNo.");
        Set<GoodStudent> mut = new HashSet<>();
        GoodStudent m = new GoodStudent("192311010", "Meena");
        mut.add(m);
        System.out.println("OBSERVED  : before change  size=" + mut.size() + " contains(m)=" + mut.contains(m));
        m.regNo = "192311099";                                // field used in hashCode changed
        System.out.println("            after change   contains(m)=" + mut.contains(m)
                + "  contains(new 192311010)=" + mut.contains(new GoodStudent("192311010", "Meena"))
                + "  contains(new 192311099)=" + mut.contains(new GoodStudent("192311099", "Meena")));
        mut.add(new GoodStudent("192311099", "Meena"));
        System.out.println("            after adding another object with regNo 192311099: size=" + mut.size() + " (same student twice!)");

        // ---------------- TEST 3b ----------------
        System.out.println();
        System.out.println("TEST 3b : TreeSet whose compare() uses name only (not consistent with equals on regNo)");
        System.out.println("PREDICTION: two DIFFERENT students with the same name 'Ravi' -> TreeSet keeps only 1 (size 1),");
        System.out.println("            although equals() says they are different.");
        Set<GoodStudent> tree = new TreeSet<>(Comparator.comparing((GoodStudent s) -> s.name));
        GoodStudent r1 = new GoodStudent("192311020", "Ravi");
        GoodStudent r2 = new GoodStudent("192311021", "Ravi");
        tree.add(r1);
        tree.add(r2);
        System.out.println("OBSERVED  : r1.equals(r2) = " + r1.equals(r2) + ", TreeSet size = " + tree.size() + ", contents = " + tree);

        // ---------------- verification of the corrected class ----------------
        System.out.println();
        System.out.println("VERIFICATION of the corrected class (equals/hashCode contract checks)");
        boolean ok = true;
        GoodStudent a = new GoodStudent("1", "A"), b = new GoodStudent("1", "B"), c = new GoodStudent("1", "C");
        ok &= a.equals(a);                                         // reflexive
        ok &= a.equals(b) == b.equals(a);                          // symmetric
        ok &= !(a.equals(b) && b.equals(c)) || a.equals(c);        // transitive
        ok &= !a.equals(null);                                     // null
        ok &= !a.equals(b) || a.hashCode() == b.hashCode();        // equal objects -> equal hash
        System.out.println("            all contract checks passed = " + ok);

        // ---------------- alternative concepts: timing for n scans ----------------
        System.out.println();
        System.out.println("=== De-duplication of n scans (half are repeat scans) : time in ms, average of 5 runs ===");
        PrintWriter csv = new PrintWriter("q3_dedup_results.csv");
        csv.println("scans,unique,HashSet_ms,TreeSet_ms,HashMap_ms");
        System.out.printf("%10s %10s %12s %12s %12s%n", "scans", "unique", "HashSet", "TreeSet", "HashMap");
        // warm up
        for (int w = 0; w < 3; w++) runAll(20000);
        for (int n : new int[]{10000, 100000, 1000000}) {
            double[] tot = new double[3];
            int unique = 0;
            for (int r = 0; r < 5; r++) {
                double[] t = runAll(n);
                for (int k = 0; k < 3; k++) tot[k] += t[k];
                unique = n / 2;
            }
            System.out.printf("%10d %10d %12.2f %12.2f %12.2f%n", n, unique, tot[0] / 5, tot[1] / 5, tot[2] / 5);
            csv.printf("%d,%d,%.3f,%.3f,%.3f%n", n, unique, tot[0] / 5, tot[1] / 5, tot[2] / 5);
        }
        csv.close();
    }

    // scans: student number (i % (n/2)), so each student is scanned twice
    static double[] runAll(int n) {
        int half = n / 2;
        String[] ids = new String[n];
        for (int i = 0; i < n; i++) ids[i] = "REG" + (i % half);
        double[] t = new double[3];

        long s = System.nanoTime();
        Set<GoodStudent> hs = new HashSet<>();
        for (int i = 0; i < n; i++) hs.add(new GoodStudent(ids[i], "x"));
        t[0] = (System.nanoTime() - s) / 1e6;

        s = System.nanoTime();
        Set<GoodStudent> ts = new TreeSet<>(Comparator.comparing((GoodStudent g) -> g.regNo));
        for (int i = 0; i < n; i++) ts.add(new GoodStudent(ids[i], "x"));
        t[1] = (System.nanoTime() - s) / 1e6;

        s = System.nanoTime();
        Map<String, GoodStudent> hm = new HashMap<>();
        for (int i = 0; i < n; i++) hm.putIfAbsent(ids[i], new GoodStudent(ids[i], "x"));
        t[2] = (System.nanoTime() - s) / 1e6;

        if (hs.size() != half || ts.size() != half || hm.size() != half) System.out.println("SIZE ERROR");
        return t;
    }
}
