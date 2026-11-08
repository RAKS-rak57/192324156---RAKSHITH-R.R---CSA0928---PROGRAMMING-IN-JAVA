package parking.model;
public class TwoWheeler extends Vehicle {
    public TwoWheeler(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 10; }
    @Override public String category() { return "TwoWheeler"; }
}
