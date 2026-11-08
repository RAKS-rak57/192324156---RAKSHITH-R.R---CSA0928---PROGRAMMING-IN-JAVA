package parking.model;
public class ElectricCar extends Car implements Chargeable {
    public ElectricCar(String regNo) { super(regNo); }
    @Override public String category() { return "ElectricCar"; }
}
