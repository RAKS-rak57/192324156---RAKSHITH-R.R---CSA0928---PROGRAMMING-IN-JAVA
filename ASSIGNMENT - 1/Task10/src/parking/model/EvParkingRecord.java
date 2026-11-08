package parking.model;

/** Subclass in the same package: can use the protected member (and, being in the same package, package-private ones). */
public class EvParkingRecord extends ParkingRecord {
    public EvParkingRecord(String regNo) { super(regNo); }
    @Override protected double ratePerHourOrDefault() { return 30; }
    public void billStandard() { bill(ratePerHourOrDefault()); }
}
