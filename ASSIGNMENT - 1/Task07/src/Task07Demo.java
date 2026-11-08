import java.util.*;

/** Task 7 - overriding: ratePerHour(), payable(), toString(), equals(), hashCode(); final; @Override rules. */
public class Task07Demo {

    // ------------------------ overriding design ------------------------
    static abstract class Vehicle {
        static final int DAILY_CAP_HOURS = 8;               // final constant: a business rule that must never change at run time
        private final String regNo;                         // final field: identity is fixed once the object exists
        protected Vehicle(String regNo) { this.regNo = regNo; }
        public final String getRegNo() { return regNo; }    // final method: no subclass may change how identity is read
        protected abstract double ratePerHour();            // the method every subclass MUST override
        public double calculateFee(int minutes) {           // NOT final: Task 12 vehicles override it (flat fee, peak pricing)
            int hours = (minutes + 59) / 60;
            return Math.min(hours * ratePerHour(), DAILY_CAP_HOURS * ratePerHour());
        }
        @Override public String toString() { return getClass().getSimpleName() + "[" + regNo + ", Rs " + ratePerHour() + "/h]"; }
        @Override public boolean equals(Object o) { return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    static class TwoWheeler extends Vehicle {
        TwoWheeler(String r) { super(r); }
        @Override protected double ratePerHour() { return 10; }
    }
    static class Car extends Vehicle {
        Car(String r) { super(r); }
        @Override protected double ratePerHour() { return 30; }
    }
    static class ElectricCar extends Car {
        ElectricCar(String r) { super(r); }
        @Override public String toString() { return super.toString() + " (EV)"; }    // overriding an inherited override
    }
    /** Example of overriding calculateFee itself: a flat-fee vehicle. */
    static class FlatFeeBike extends Vehicle {
        FlatFeeBike(String r) { super(r); }
        @Override protected double ratePerHour() { return 15; }
        @Override public double calculateFee(int minutes) { return minutes == 0 ? 0 : 15; }   // same signature, same return type, access not reduced
    }

    static abstract class User {
        abstract double payable(double fee);
    }
    static class PassHolder extends User { @Override double payable(double fee) { return 0; } }
    static class Visitor extends User { @Override double payable(double fee) { return fee; } }

    // ------------------------ alternative: switch on a type field ------------------------
    static class SwitchVehicle {
        static final int TWO = 0, CAR = 1, EV = 2;
        final String regNo; final int type;
        SwitchVehicle(String r, int type) { this.regNo = r; this.type = type; }
        double calculateFee(int minutes) {
            double rate;
            switch (type) {                       // EVERY new vehicle type means editing this method
                case TWO: rate = 10; break;
                case CAR: rate = 30; break;
                case EV:  rate = 30; break;
                default: throw new IllegalStateException("unknown type " + type);
            }
            int hours = (minutes + 59) / 60;
            return Math.min(hours * rate, 8 * rate);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== toString() ===");
        Vehicle[] vs = { new TwoWheeler("TN01A1"), new Car("TN02B2"), new ElectricCar("TN03C3"), new FlatFeeBike("TN04D4") };
        for (Vehicle v : vs) System.out.println(v);

        System.out.println();
        System.out.println("=== calculateFee overridden vs inherited (130 minutes) ===");
        for (Vehicle v : vs) System.out.println(v.getRegNo() + " -> Rs " + v.calculateFee(130));

        System.out.println();
        System.out.println("=== equals() and hashCode() ===");
        Vehicle a = new Car("TN02B2"), b = new ElectricCar("TN02B2"), c = new Car("TN99Z9");
        System.out.println("Car(TN02B2).equals(ElectricCar(TN02B2)) = " + a.equals(b));
        System.out.println("symmetric: ElectricCar.equals(Car)      = " + b.equals(a));
        System.out.println("Car(TN02B2).equals(Car(TN99Z9))         = " + a.equals(c));
        System.out.println("hashCodes equal for equal objects       = " + (a.hashCode() == b.hashCode()));
        Set<Vehicle> parked = new HashSet<>(List.of(a, c));
        System.out.println("HashSet contains ElectricCar(TN02B2)?     " + parked.contains(b) + "  -> duplicate registration detected");
        System.out.println("without equals/hashCode the default Object versions compare memory addresses and the answer would be false.");

        System.out.println();
        System.out.println("=== User.payable() overridden ===");
        System.out.println("PassHolder pays Rs " + new PassHolder().payable(90) + ", Visitor pays Rs " + new Visitor().payable(90));

        System.out.println();
        System.out.println("=== final ===");
        System.out.println("Vehicle.DAILY_CAP_HOURS (static final) = " + Vehicle.DAILY_CAP_HOURS + "; regNo is a final field; getRegNo() is a final method.");
        System.out.println("(see negative/ for what the compiler says when these rules are broken)");

        System.out.println();
        System.out.println("=== Alternative: switch on a type field ===");
        SwitchVehicle s = new SwitchVehicle("TN02B2", SwitchVehicle.CAR);
        System.out.println("switch version, car, 130 min = Rs " + s.calculateFee(130));

        int n = 3000; Vehicle[] ov = new Vehicle[n]; SwitchVehicle[] sv = new SwitchVehicle[n];
        for (int i = 0; i < n; i++) {
            int t = i % 3;
            ov[i] = t == 0 ? new TwoWheeler("T" + i) : t == 1 ? new Car("C" + i) : new ElectricCar("E" + i);
            sv[i] = new SwitchVehicle("V" + i, t);
        }
        double sink = 0; int reps = 20_000;
        for (int w = 0; w < 4; w++) {
            long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (Vehicle v : ov) sink += v.calculateFee(130);
            long t1 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (SwitchVehicle v : sv) sink += v.calculateFee(130);
            long t2 = System.nanoTime();
            if (w == 3) System.out.printf("time per fee (ns), k = 3 types: overriding = %.2f, switch = %.2f (sink=%d)%n",
                    (t1 - t0) / (double) ((long) reps * n), (t2 - t1) / (double) ((long) reps * n), sink > 0 ? 1 : 0);
        }
        System.out.println("overriding: virtual dispatch via vtable = O(1) for any k;  switch: compiled to tableswitch = O(1), if-else chain = O(k)");
        System.out.println("add a new type: overriding -> 1 NEW class, 0 classes changed;  switch -> constants + switch edited in the EXISTING class (1+ classes changed, re-test everything)");
    }
}
