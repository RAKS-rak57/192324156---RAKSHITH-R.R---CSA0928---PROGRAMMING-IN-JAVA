import java.util.*;

/** Task 9 - abstract class for the shared IS-A core, interfaces for optional CAPABILITIES. */
public class Task09Demo {

    /** Capability: only some vehicles can be charged. Includes a default and a static method. */
    interface Chargeable {
        double RATE_PER_KWH = 12;                              // interface constants are public static final
        double chargingFee(int kWh);
        default double chargingFeeWithGst(int kWh) {           // default method: shared behaviour, no need to repeat it in every EV class
            return chargingFee(kWh) * 1.18;
        }
        static boolean isValidKwh(int kWh) { return kWh >= 0 && kWh <= 200; }   // static method: utility owned by the interface
    }

    /** Capability: somebody who must pay a fee. */
    interface Payable {
        double amountDue(double fee);
        String payerName();
    }

    /** Abstract class: holds STATE (regNo), a constructor and the template method; leaves rate abstract. */
    static abstract class Vehicle {
        private final String regNo;
        protected Vehicle(String regNo) { this.regNo = regNo; }
        public String getRegNo() { return regNo; }
        protected abstract double ratePerHour();                              // WHAT varies
        public double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }   // HOW it is done, written once
    }
    static class Car extends Vehicle {
        Car(String r) { super(r); }
        @Override protected double ratePerHour() { return 30; }
    }
    /** One class, several behaviours: extends ONE class, implements MANY interfaces. */
    static class ElectricCar extends Car implements Chargeable, Comparable<ElectricCar> {
        ElectricCar(String r) { super(r); }
        @Override public double chargingFee(int kWh) { return kWh * RATE_PER_KWH; }
        @Override public int compareTo(ElectricCar o) { return getRegNo().compareTo(o.getRegNo()); }
    }
    static class Visitor implements Payable {
        private final String name;
        Visitor(String n) { name = n; }
        @Override public double amountDue(double fee) { return fee; }
        @Override public String payerName() { return name; }
    }
    static class MonthlyPassHolder implements Payable {
        private final String name;
        MonthlyPassHolder(String n) { name = n; }
        @Override public double amountDue(double fee) { return 0; }
        @Override public String payerName() { return name; }
    }

    // ------------- Alternative: Vehicle as an interface, Chargeable as an abstract class -------------
    interface VehicleI {
        String getRegNo();                                    // no field can live here, so each class must keep its own regNo
        double ratePerHour();                                 // note: interface methods are implicitly PUBLIC
        default double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }
    }
    static abstract class ChargeableA {
        abstract double chargingFee(int kWh);
    }
    static class CarI implements VehicleI {
        private final String regNo;                           // duplicated state: every implementer repeats this
        CarI(String r) { regNo = r; }                         // no constructor sharing, no super(regNo)
        @Override public String getRegNo() { return regNo; }
        @Override public double ratePerHour() { return 30; }
    }
    static class ElectricCarI extends ChargeableA implements VehicleI {   // OK only because VehicleI is an interface
        private final String regNo;
        ElectricCarI(String r) { regNo = r; }
        @Override public String getRegNo() { return regNo; }
        @Override public double ratePerHour() { return 30; }
        @Override double chargingFee(int kWh) { return kWh * 12; }
        // but it can no longer extend CarI (is-a Car) AND ChargeableA - Java has single class inheritance
    }

    public static void main(String[] args) {
        System.out.println("=== Abstract class: template method + abstract hook ===");
        Vehicle v = new Car("TN02B2");
        System.out.println("Car fee(130) = " + v.calculateFee(130));
        // Vehicle bad = new Vehicle("X");   // compile error: Vehicle is abstract; cannot be instantiated

        System.out.println();
        System.out.println("=== Interfaces: multiple capabilities ===");
        ElectricCar ev = new ElectricCar("TN03C3");
        System.out.println("ElectricCar is a Vehicle: " + (ev instanceof Vehicle) + ", Chargeable: " + (ev instanceof Chargeable) + ", Comparable: " + (ev instanceof Comparable));
        System.out.println("chargingFee(10 kWh)         = " + ev.chargingFee(10));
        System.out.println("chargingFeeWithGst(10 kWh)  = " + ev.chargingFeeWithGst(10) + "   (default method, written once in the interface)");
        System.out.println("Chargeable.isValidKwh(250)  = " + Chargeable.isValidKwh(250) + "   (static interface method)");
        Payable[] payers = { new Visitor("Guest"), new MonthlyPassHolder("Asha") };
        for (Payable p : payers) System.out.println(p.payerName() + " owes Rs " + p.amountDue(90));
        List<ElectricCar> evs = new ArrayList<>(List.of(new ElectricCar("TN09"), new ElectricCar("TN01")));
        Collections.sort(evs);
        System.out.println("sorted via Comparable: " + evs.get(0).getRegNo() + ", " + evs.get(1).getRegNo());

        System.out.println();
        System.out.println("=== Alternative design (Vehicle = interface, Chargeable = abstract class) ===");
        VehicleI c = new CarI("TN02B2"); ElectricCarI e = new ElectricCarI("TN03C3");
        System.out.println("CarI fee(130) = " + c.calculateFee(130) + ", ElectricCarI fee(130) = " + e.calculateFee(130) + ", charge(10) = " + e.chargingFee(10));
        System.out.println("Works, but: regNo is duplicated in every class, no constructor is shared, ElectricCarI is NOT a CarI,");
        System.out.println("and any class that needs Chargeable can no longer extend another class. See negative/ for the compiler errors.");

        // timing: interface default method vs abstract class method
        VehicleI[] ia = new VehicleI[1000]; Vehicle[] aa = new Vehicle[1000];
        for (int i = 0; i < 1000; i++) { ia[i] = new CarI("C" + i); aa[i] = new Car("C" + i); }
        double sink = 0; int reps = 30_000;
        for (int w = 0; w < 4; w++) {
            long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (Vehicle x : aa) sink += x.calculateFee(130);
            long t1 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (VehicleI x : ia) sink += x.calculateFee(130);
            long t2 = System.nanoTime();
            if (w == 3) System.out.printf("%ntime per call (ns): abstract-class method = %.2f, interface default method = %.2f (sink=%d)%n",
                    (t1 - t0) / (double) ((long) reps * 1000), (t2 - t1) / (double) ((long) reps * 1000), sink > 0 ? 1 : 0);
        }
        System.out.println("Both are O(1) per call. Space per object: abstract class = 1 regNo field in the one superclass;");
        System.out.println("interface version = 1 regNo field in EACH implementing class (same bytes per object, but the field and constructor code are repeated).");
    }
}
