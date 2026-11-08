package parking.model;
/** E-bike: ONE flat fee per visit, whatever the duration (zero-minute stays are still free). */
public class EBike extends Vehicle {
    public static final double FLAT_FEE = 15;
    public EBike(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return FLAT_FEE; }     // required by the abstract base; not used for billing
    @Override public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Negative duration: " + minutes);
        return minutes == 0 ? 0 : FLAT_FEE;
    }
    @Override public String category() { return "EBike"; }
}
