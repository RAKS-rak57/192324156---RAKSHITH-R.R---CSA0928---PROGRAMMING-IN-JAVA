package parking.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Delivery van: DOUBLE the car rate for any billable hour that STARTS between 09:00 (inclusive) and 17:00 (exclusive).
 * Extends Car so that a future change of the car rate flows through automatically.
 */
public class DeliveryVan extends Car {
    public static final LocalTime PEAK_START = LocalTime.of(9, 0);
    public static final LocalTime PEAK_END = LocalTime.of(17, 0);
    public static final int PEAK_MULTIPLIER = 2;

    public DeliveryVan(String regNo) { super(regNo); }

    private static boolean isPeak(LocalDateTime t) {
        LocalTime lt = t.toLocalTime();
        return !lt.isBefore(PEAK_START) && lt.isBefore(PEAK_END);
    }

    @Override public double calculateFee(LocalDateTime in, LocalDateTime out) {
        if (out.isBefore(in)) throw new IllegalArgumentException("Exit time is before entry time");
        long minutes = Duration.between(in, out).toMinutes();
        if (minutes == 0) return 0;
        int hours = (int) ((minutes + 59) / 60);
        double cap = DAILY_CAP_HOURS * PEAK_MULTIPLIER * ratePerHour();     // per 24-hour block
        double total = 0, block = 0;
        int inBlock = 0;
        for (int i = 0; i < hours; i++) {
            LocalDateTime hourStart = in.plusHours(i);
            block += isPeak(hourStart) ? PEAK_MULTIPLIER * ratePerHour() : ratePerHour();
            inBlock++;
            if (inBlock == 24 || i == hours - 1) { total += Math.min(block, cap); block = 0; inBlock = 0; }
        }
        return total;
    }
    @Override public String category() { return "DeliveryVan"; }
}
