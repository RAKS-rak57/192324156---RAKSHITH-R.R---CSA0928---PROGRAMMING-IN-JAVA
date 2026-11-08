import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Q4 - Choosing a Map for the hospital pharmacy inventory.
 * Requirements: (1) fast lookup by medicine code, (2) report in code order,
 * (3) report in the order medicines were added.
 */
public class PharmacyInventory {

    static class Medicine {
        String code;
        String name;
        int stock;
        int addedOrder;          // used only when the "wrong" map has to produce the added-order report
        Medicine(String code, String name, int stock, int addedOrder) {
            this.code = code; this.name = name; this.stock = stock; this.addedOrder = addedOrder;
        }
        public String toString() { return code + "(" + stock + ")"; }
    }

    static final int REPEATS = 5;
    static final int LOOKUPS = 100000;
    static long sink = 0;

    // codes are added in RANDOM order so that "code order" and "added order" are different
    static Medicine[] makeMedicines(int n, long seed) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < n; i++) numbers.add(i);
        Collections.shuffle(numbers, new Random(seed));
        Medicine[] meds = new Medicine[n];
        for (int i = 0; i < n; i++) {
            String code = String.format("MED%07d", numbers.get(i));
            meds[i] = new Medicine(code, "Drug" + i, 10 + i % 500, i);
        }
        return meds;
    }

    static double ms(long start) { return (System.nanoTime() - start) / 1e6; }

    public static void main(String[] args) throws Exception {
        int[] sizes = {1000, 20000, 500000};

        // ----- small correctness demonstration -----
        System.out.println("=== Q4 : order demonstration with 6 medicines (added in this order) ===");
        String[] demo = {"M305", "M102", "M550", "M101", "M420", "M210"};
        Map<String, Integer> h = new HashMap<>(), t = new TreeMap<>(), l = new LinkedHashMap<>();
        for (int i = 0; i < demo.length; i++) { h.put(demo[i], i); t.put(demo[i], i); l.put(demo[i], i); }
        System.out.println("Added order       : " + Arrays.toString(demo));
        System.out.println("HashMap keys      : " + h.keySet() + "   (no guaranteed order)");
        System.out.println("TreeMap keys      : " + t.keySet() + "   (code order)");
        System.out.println("LinkedHashMap keys: " + l.keySet() + "   (added order)");

        // warm up
        for (int w = 0; w < 3; w++) runOnce(makeMedicines(20000, 1), null);

        PrintWriter csv = new PrintWriter("q4_map_results.csv");
        csv.println("size,structure,put_ms,lookup_100000_ms,ns_per_lookup,iterate_ms,code_order_report_ms,added_order_report_ms,code_order_correct,added_order_correct");

        System.out.println();
        System.out.println("=== Timings (ms, average of " + REPEATS + " runs). Lookups = " + LOOKUPS + " ===");
        for (int n : sizes) {
            Medicine[] meds = makeMedicines(n, 7);
            double[][] sum = new double[5][7];       // 5 structures x 7 numbers
            boolean[][] ok = new boolean[5][2];
            for (int r = 0; r < REPEATS; r++) {
                double[][] one = new double[5][7];
                runOnce(meds, one, ok);
                for (int s = 0; s < 5; s++) for (int k = 0; k < 7; k++) sum[s][k] += one[s][k];
            }
            String[] names = {"HashMap", "TreeMap", "LinkedHashMap", "Hashtable", "SortedArray+binarySearch"};
            System.out.println();
            System.out.println("Number of medicines N = " + n);
            System.out.printf("%-26s %9s %12s %10s %10s %14s %14s%n", "Structure", "put", "100k lookups", "ns/lookup", "iterate", "code-order rpt", "added-order rpt");
            for (int s = 0; s < 5; s++) {
                double put = sum[s][0] / REPEATS, look = sum[s][1] / REPEATS, it = sum[s][2] / REPEATS;
                double rc = sum[s][3] / REPEATS, ra = sum[s][4] / REPEATS;
                System.out.printf("%-26s %9.2f %12.2f %10.1f %10.2f %14.2f %14.2f%n", names[s], put, look, look * 1e6 / LOOKUPS, it, rc, ra);
                csv.printf("%d,%s,%.3f,%.3f,%.1f,%.3f,%.3f,%.3f,%b,%b%n", n, names[s], put, look, look * 1e6 / LOOKUPS, it, rc, ra, ok[s][0], ok[s][1]);
            }
            System.out.println("Order matches requirement (code report / added report):");
            for (int s = 0; s < 5; s++) System.out.println("   " + names[s] + " : " + ok[s][0] + " / " + ok[s][1]);
        }
        csv.close();
        System.out.println();
        System.out.println("(checksum " + sink + ")");
    }

    static void runOnce(Medicine[] meds, double[][] out) { runOnce(meds, out, new boolean[5][2]); }

    // out[s][0]=put, [1]=lookups, [2]=iterate, [3]=code-order report, [4]=added-order report
    static void runOnce(Medicine[] meds, double[][] out, boolean[][] ok) {
        int n = meds.length;
        Random rnd = new Random(99);
        String[] lookupCodes = new String[LOOKUPS];
        for (int i = 0; i < LOOKUPS; i++) lookupCodes[i] = meds[rnd.nextInt(n)].code;

        // the two correct answers (built once, used to check each report)
        String[] codeOrder = new String[n];
        String[] addedOrder = new String[n];
        for (int i = 0; i < n; i++) { codeOrder[i] = meds[i].code; addedOrder[i] = meds[i].code; }
        Arrays.sort(codeOrder);

        // ---- 0 HashMap, 1 TreeMap, 2 LinkedHashMap, 3 Hashtable (all through the Map interface) ----
        for (int s = 0; s < 4; s++) {
            Map<String, Medicine> map;
            if (s == 0) map = new HashMap<>();
            else if (s == 1) map = new TreeMap<>();
            else if (s == 2) map = new LinkedHashMap<>();
            else map = new Hashtable<>();

            long st = System.nanoTime();
            for (Medicine m : meds) map.put(m.code, m);
            double put = ms(st);

            st = System.nanoTime();
            for (int i = 0; i < LOOKUPS; i++) sink += map.get(lookupCodes[i]).stock;
            double look = ms(st);

            st = System.nanoTime();
            for (Medicine m : map.values()) sink += m.stock;
            double it = ms(st);

            // report 1 : medicines in CODE order
            st = System.nanoTime();
            List<String> rep1 = new ArrayList<>();
            if (s == 1) {
                rep1.addAll(map.keySet());                       // TreeMap: already sorted
            } else {
                rep1.addAll(map.keySet());                       // extra work: sort the keys
                Collections.sort(rep1);
            }
            double r1 = ms(st);

            // report 2 : medicines in ADDED order
            st = System.nanoTime();
            List<String> rep2 = new ArrayList<>();
            if (s == 2) {
                rep2.addAll(map.keySet());                       // LinkedHashMap: already in added order
            } else {
                List<Medicine> list = new ArrayList<>(map.values());   // extra work: sort by addedOrder
                Collections.sort(list, Comparator.comparingInt((Medicine m) -> m.addedOrder));
                for (Medicine m : list) rep2.add(m.code);
            }
            double r2 = ms(st);

            if (out != null) {
                out[s][0] = put; out[s][1] = look; out[s][2] = it; out[s][3] = r1; out[s][4] = r2;
            }
            ok[s][0] = rep1.equals(Arrays.asList(codeOrder));
            ok[s][1] = rep2.equals(Arrays.asList(addedOrder));
        }

        // ---- 4 sorted array + Arrays.binarySearch (alternative concept) ----
        long st = System.nanoTime();
        Medicine[] sorted = meds.clone();
        Arrays.sort(sorted, Comparator.comparing((Medicine m) -> m.code));   // building = one sort
        double put = ms(st);

        Medicine key = new Medicine("", "", 0, 0);
        st = System.nanoTime();
        for (int i = 0; i < LOOKUPS; i++) {
            key.code = lookupCodes[i];
            int pos = Arrays.binarySearch(sorted, key, Comparator.comparing((Medicine m) -> m.code));
            sink += sorted[pos].stock;
        }
        double look = ms(st);

        st = System.nanoTime();
        for (Medicine m : sorted) sink += m.stock;
        double it = ms(st);

        st = System.nanoTime();
        List<String> rep1 = new ArrayList<>();
        for (Medicine m : sorted) rep1.add(m.code);               // already in code order
        double r1 = ms(st);

        st = System.nanoTime();
        Medicine[] copy = sorted.clone();
        Arrays.sort(copy, Comparator.comparingInt((Medicine m) -> m.addedOrder));
        List<String> rep2 = new ArrayList<>();
        for (Medicine m : copy) rep2.add(m.code);
        double r2 = ms(st);

        if (out != null) { out[4][0] = put; out[4][1] = look; out[4][2] = it; out[4][3] = r1; out[4][4] = r2; }
        ok[4][0] = rep1.equals(Arrays.asList(codeOrder));
        ok[4][1] = rep2.equals(Arrays.asList(addedOrder));
    }
}
