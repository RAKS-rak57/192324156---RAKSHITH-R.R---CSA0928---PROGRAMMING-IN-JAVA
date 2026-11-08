import java.util.*;

/**
 * Task 2 - Data representation, type conversion and input.
 *  Part 1: read input with Scanner (stdin / file redirect)
 *  Part 2: integer-division / cast bugs that give a WRONG fee and the fix
 *  Part 3: jagged array vs flat 1-D array vs ArrayList<String[]>
 */
public class Task02Demo {

    // ---- Fee computations -------------------------------------------------
    static double wrongFeeIntDivision(int minutes, double rate) {
        int hours = minutes / 60;                  // 130/60 = 2  (fraction thrown away)
        return hours * rate;
    }
    static double wrongFeeLateCast(int minutes, double rate) {
        double hours = (double) (minutes / 60);    // cast applied AFTER the integer division -> still 2.0
        return Math.ceil(hours) * rate;
    }
    static double wrongFeeTruncatingCast(int minutes, double rate) {
        int hours = (int) (minutes / 60.0) ;       // (int) truncates 2.17 -> 2
        return hours * rate;
    }
    static double correctFee(int minutes, double rate) {
        int hours = (minutes + 59) / 60;           // ceiling using integer arithmetic only
        return hours * rate;
    }
    static double correctFeeDoubleCeil(int minutes, double rate) {
        return Math.ceil(minutes / 60.0) * rate;   // (double) division first, then ceil
    }

    // ---- Locating a slot ---------------------------------------------------
    static String locateJagged(String[][] s, int f, int k) { return s[f][k]; }
    static String locateList(ArrayList<String[]> s, int f, int k) { return s.get(f)[k]; }
    static String locateFlat(String[] flat, int[] offset, int f, int k) { return flat[offset[f] + k]; }

    public static void main(String[] args) {
        // ---------- Part 1 : input with Scanner ----------
        System.out.println("=== Part 1: reading input with Scanner (input/input.txt) ===");
        Scanner sc = new Scanner(System.in);
        int count = sc.nextInt();                  // number of vehicles
        List<String> regs = new ArrayList<>();
        List<Integer> mins = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String reg = sc.next();
            String type = sc.next();
            int minutes = sc.nextInt();
            double rate = switch (type) { case "TWO" -> 10; case "CAR" -> 30; default -> 30; };
            regs.add(reg); mins.add(minutes);
            System.out.printf("%-12s %-4s %4d min  correct fee = Rs %.1f%n", reg, type, minutes, correctFee(minutes, rate));
        }
        sc.close();

        // ---------- Part 2 : wrong fee from integer division / casts ----------
        System.out.println();
        System.out.println("=== Part 2: casting risks (Car, Rs 30/hour) ===");
        System.out.printf("%-8s %-12s %-12s %-14s %-10s %-10s%n",
                "minutes", "int-division", "late cast", "truncating", "correct", "ceil(dbl)");
        int[] test = {1, 59, 60, 61, 130, 150};
        for (int m : test) {
            System.out.printf("%-8d %-12.1f %-12.1f %-14.1f %-10.1f %-10.1f%n", m,
                    wrongFeeIntDivision(m, 30), wrongFeeLateCast(m, 30), wrongFeeTruncatingCast(m, 30),
                    correctFee(m, 30), correctFeeDoubleCeil(m, 30));
        }
        System.out.println("-> 130 minutes: int division bills Rs 60 instead of Rs 90 (loses Rs 30 = 33% of the fee).");

        long entryMillis = 1_762_500_000_000L;     // an epoch-millis timestamp (Nov 2025)
        int badCast = (int) entryMillis;
        System.out.println("long->int narrowing: " + entryMillis + " becomes " + badCast + "  (overflow) -> keep timestamps in long/LocalDateTime");

        double d = 0.1 + 0.2;
        System.out.println("double money risk: 0.1 + 0.2 = " + d + "  -> fees are whole rupees; use int paise or BigDecimal for fractional money");
        System.out.println("widening is safe: int hours = 3; double fee = hours * 30.5 = " + (3 * 30.5));

        // ---------- Part 3 : jagged vs flat vs ArrayList ----------
        System.out.println();
        System.out.println("=== Part 3: representing floors and slots ===");
        int[] slotsPerFloor = {20, 35, 50, 75, 100};          // different slots per floor
        String[][] jagged = new String[slotsPerFloor.length][];
        ArrayList<String[]> list = new ArrayList<>();
        int[] offset = new int[slotsPerFloor.length + 1];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            jagged[f] = new String[slotsPerFloor[f]];
            list.add(new String[slotsPerFloor[f]]);
            offset[f + 1] = offset[f] + slotsPerFloor[f];
        }
        String[] flat = new String[offset[slotsPerFloor.length]];
        jagged[3][40] = "TN09ZZ9999"; list.get(3)[40] = "TN09ZZ9999"; flat[offset[3] + 40] = "TN09ZZ9999";
        System.out.println("Facility 5 floors, slots = " + Arrays.toString(slotsPerFloor) + ", total = " + flat.length);
        System.out.println("jagged[3][40]            = " + locateJagged(jagged, 3, 40));
        System.out.println("list.get(3)[40]          = " + locateList(list, 3, 40));
        System.out.println("flat[offset[3] + 40]     = " + locateFlat(flat, offset, 3, 40));
        int naive = 3 * 20 + 40;   // formula floor*slotsPerFloor+slot with slotsPerFloor=20 is WRONG for uneven floors
        System.out.println("naive index 3*20+40 = " + naive + " -> flat[" + naive + "] = " + flat[naive] + "   (WRONG slot: formula only works if every floor has the same size)");

        // timing: 20 million lookups each
        int reps = 20_000_000; long sink = 0; // prevents the JIT from removing the loops
        for (int w = 0; w < 3; w++) {
            long a0 = System.nanoTime();
            for (int i = 0; i < reps; i++) { String s = locateJagged(jagged, i % 5, i % 20); if (s != null) sink++; }
            long a1 = System.nanoTime();
            for (int i = 0; i < reps; i++) { String s = locateList(list, i % 5, i % 20); if (s != null) sink++; }
            long a2 = System.nanoTime();
            for (int i = 0; i < reps; i++) { String s = locateFlat(flat, offset, i % 5, i % 20); if (s != null) sink++; }
            long a3 = System.nanoTime();
            if (w == 2) System.out.printf("Locate time per lookup (ns): jagged = %.2f, ArrayList<String[]> = %.2f, flat+offset = %.2f%n",
                    (a1 - a0) / (double) reps, (a2 - a1) / (double) reps, (a3 - a2) / (double) reps);
        }
        System.out.println("Space: jagged = F array objects + total slots refs; flat = 1 array (total refs) + offset[F+1]; ArrayList = F arrays + list backing array");
    }
}
