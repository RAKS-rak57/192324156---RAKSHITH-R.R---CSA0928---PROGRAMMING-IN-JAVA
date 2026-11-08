interface Chargeable {
    double chargingFee(int kWh);
}

abstract class Vehicle {
    private final String regNo;
    protected Vehicle(String regNo) { this.regNo = regNo; }
    public String getRegNo() { return regNo; }
    protected abstract double ratePerHour();

    /** Every started hour is charged in full. */
    public double calculateFee(int minutes) {
        // TODO 1: Compute billable hours as the ceiling of minutes / 60 using integer
        // arithmetic only, and return hours × ratePerHour()
        int hours = (minutes + 59) / 60;          // ceiling(minutes / 60) without any double
        return hours * ratePerHour();
    }

    @Override public boolean equals(Object o) {
        return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
}

class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 10; }
}

class Car extends Vehicle {
    // TODO 2: Write the constructor using super, and override ratePerHour() to return
    // 30
    Car(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 30; }
}

class ElectricCar extends Car implements Chargeable {
    ElectricCar(String regNo) { super(regNo); }
    // TODO 3: Implement chargingFee() at Rs 12 per kWh
    @Override public double chargingFee(int kWh) { return kWh * 12; }
}

public class FeeDemo {
    public static void main(String[] args) {
        Vehicle[] parked = { new TwoWheeler("TN01A1"), new Car("TN02B2"),
                new ElectricCar("TN03C3") };
        for (Vehicle v : parked) {
            double fee = v.calculateFee(130); // dynamic method dispatch
            if (v instanceof Chargeable c) fee += c.chargingFee(10);
            System.out.println(v.getClass().getSimpleName() + " " + v.getRegNo()
                    + " -> Rs " + fee);
        }
        System.out.println(new Car("TN02B2").equals(new ElectricCar("TN02B2")));
    }
}
