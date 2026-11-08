package parking.model;
public class Car extends Vehicle {
    public Car(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 30; }
    @Override public String category() { return "Car"; }
}
