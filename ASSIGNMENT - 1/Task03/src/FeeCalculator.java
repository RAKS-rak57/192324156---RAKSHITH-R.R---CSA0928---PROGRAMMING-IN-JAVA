/**
 * Task 3 - fee slabs with switch + if-else (Java version of the pseudocode in the report).
 *  slab 1: 0 minutes            -> free (no time used)
 *  slab 2: every STARTED hour   -> charged in full at the hourly rate
 *  slab 3: daily maximum        -> a 24-hour block never costs more than 8 hours' worth
 */
public class FeeCalculator {
    static final int MINUTES_PER_DAY = 1440;
    static final int DAILY_CAP_HOURS = 8;

    static double rateFor(String type) {
        switch (type) {                       // classic switch with break/return
            case "TWO_WHEELER": return 10;
            case "CAR":         return 30;
            case "EV":          return 30;
            default: throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }

    static double computeFee(String type, int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Negative duration");
        if (minutes == 0) return 0;                          // early return: no work needed
        double rate = rateFor(type);
        double cap = DAILY_CAP_HOURS * rate;
        int fullDays = minutes / MINUTES_PER_DAY;            // integer division is intended here
        int remainder = minutes % MINUTES_PER_DAY;
        int hours = (remainder + 59) / 60;                   // ceiling, integer arithmetic only
        double remainderFee = hours * rate;
        if (remainderFee > cap) remainderFee = cap;          // daily maximum slab
        return fullDays * cap + remainderFee;
    }

    public static void main(String[] args) {
        int[] minutes = {0, 1, 59, 60, 61, 120, 130, 480, 481, 720, 1439, 1440, 1441, 2880 + 90};
        System.out.printf("%-8s %-12s %-8s %-8s%n", "minutes", "TWO_WHEELER", "CAR", "EV");
        for (int m : minutes) {
            System.out.printf("%-8d %-12.1f %-8.1f %-8.1f%n", m,
                    computeFee("TWO_WHEELER", m), computeFee("CAR", m), computeFee("EV", m));
        }
        try { computeFee("BUS", 30); } catch (IllegalArgumentException e) { System.out.println("BUS -> " + e.getMessage()); }
    }
}
