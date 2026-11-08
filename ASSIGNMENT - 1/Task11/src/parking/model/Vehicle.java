package parking.model;

/** Abstract base of every vehicle: shared state + the fee algorithm; subclasses supply the rate. */
public abstract class Vehicle {
    public static final int MINUTES_PER_DAY = 1440;
    public static final int DAILY_CAP_HOURS = 8;

    private final String regNo;

    protected Vehicle(String regNo) {
        if (regNo == null || !regNo.matches("[A-Z0-9]{4,12}"))
            throw new IllegalArgumentException("Invalid registration number: " + regNo);
        this.regNo = regNo;
    }

    public final String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    public abstract String category();

    /** Every started hour is charged in full; a 24-hour block never costs more than DAILY_CAP_HOURS hours. */
    public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Negative duration: " + minutes);
        if (minutes == 0) return 0;
        double rate = ratePerHour();
        double cap = DAILY_CAP_HOURS * rate;
        int fullDays = minutes / MINUTES_PER_DAY;
        int remainder = minutes % MINUTES_PER_DAY;
        int hours = (remainder + 59) / 60;                // ceiling with integer arithmetic only
        return fullDays * cap + Math.min(hours * rate, cap);
    }

    @Override public String toString() { return category() + "[" + regNo + "]"; }
    @Override public boolean equals(Object o) { return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
}
