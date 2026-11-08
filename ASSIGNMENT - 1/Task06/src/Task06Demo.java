import java.time.LocalDate;

/** Task 6 - inheritance hierarchy for vehicles and users, with a constructor-order trace. */
public class Task06Demo {

    static boolean trace = true;                       // switched off during the timing loop
    static void log(String m) { if (trace) System.out.println(m); }

    // ---------------- Vehicle hierarchy (multilevel + hierarchical) ----------------
    static abstract class Vehicle {
        private final String regNo;
        protected Vehicle(String regNo) {
            log("  [1] Vehicle constructor     : regNo = " + regNo);
            this.regNo = regNo;
        }
        public String getRegNo() { return regNo; }
        protected abstract double ratePerHour();
        public String describe() { return getClass().getSimpleName() + " " + regNo + " @ Rs " + ratePerHour() + "/h"; }
        public double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }
    }
    static class TwoWheeler extends Vehicle {
        TwoWheeler(String r) { super(r); log("  [2] TwoWheeler constructor"); }
        @Override protected double ratePerHour() { return 10; }
    }
    static class Car extends Vehicle {
        Car(String r) { super(r); log("  [2] Car constructor"); }
        @Override protected double ratePerHour() { return 30; }
    }
    static class ElectricCar extends Car {
        private final int batteryKwh;
        ElectricCar(String r, int batteryKwh) {
            super(r);                                  // runs Car -> Vehicle FIRST
            this.batteryKwh = batteryKwh;
            log("  [3] ElectricCar constructor : batteryKwh = " + batteryKwh);
        }
        public int getBatteryKwh() { return batteryKwh; }
        @Override public String describe() { return super.describe() + " (EV, " + batteryKwh + " kWh)"; }   // super.method()
    }

    // ---------------- User hierarchy (hierarchical) ----------------
    static abstract class User {
        protected final String name;
        User(String name) { this.name = name; }
        abstract boolean isFeeExempt(LocalDate on);
        abstract String role();
    }
    static class Student extends User {
        private final LocalDate passExpiry;
        Student(String n, LocalDate passExpiry) { super(n); this.passExpiry = passExpiry; }
        @Override boolean isFeeExempt(LocalDate on) { return passExpiry != null && !on.isAfter(passExpiry); }
        @Override String role() { return "Student"; }
    }
    static class Staff extends User {
        private final LocalDate passExpiry;
        Staff(String n, LocalDate passExpiry) { super(n); this.passExpiry = passExpiry; }
        @Override boolean isFeeExempt(LocalDate on) { return passExpiry != null && !on.isAfter(passExpiry); }
        @Override String role() { return "Staff"; }
    }
    static class Visitor extends User {
        Visitor(String n) { super(n); }
        @Override boolean isFeeExempt(LocalDate on) { return false; }
        @Override String role() { return "Visitor"; }
    }

    // ---------------- Alternative: composition (has-a VehicleType) ----------------
    static class VehicleType {
        final String name; final double rate;
        VehicleType(String name, double rate) { this.name = name; this.rate = rate; }
    }
    static class VehicleC {
        private final String regNo; private final VehicleType type;       // has-a
        VehicleC(String regNo, VehicleType type) { this.regNo = regNo; this.type = type; }
        double calculateFee(int minutes) { return ((minutes + 59) / 60) * type.rate; }
        String describe() { return type.name + " " + regNo; }
    }

    public static void main(String[] args) {
        System.out.println("=== Constructor trace: new ElectricCar(\"TN09EV0001\", 40) ===");
        ElectricCar ev = new ElectricCar("TN09EV0001", 40);
        System.out.println("  -> order of execution: Vehicle (top) -> Car -> ElectricCar (bottom)");
        System.out.println();
        System.out.println("=== Constructor trace: new TwoWheeler(\"TN01A1\") ===");
        Vehicle bike = new TwoWheeler("TN01A1");
        System.out.println();
        System.out.println("=== Inherited and specialised members ===");
        System.out.println(bike.describe());
        System.out.println(ev.describe());
        System.out.println("ElectricCar inherits getRegNo() from Vehicle: " + ev.getRegNo());
        System.out.println("fee(130 min): bike = " + bike.calculateFee(130) + ", ev = " + ev.calculateFee(130));

        System.out.println();
        System.out.println("=== Users ===");
        LocalDate day = LocalDate.of(2026, 11, 7);
        User[] users = { new Student("Asha", LocalDate.of(2026, 12, 31)), new Staff("Dr. Rao", LocalDate.of(2026, 10, 31)), new Visitor("Guest") };
        for (User u : users) System.out.println(u.role() + " " + u.name + " fee-exempt on " + day + " = " + u.isFeeExempt(day));

        System.out.println();
        System.out.println("=== Alternative: composition ===");
        VehicleType car = new VehicleType("Car", 30), bikeT = new VehicleType("TwoWheeler", 10);
        VehicleC c1 = new VehicleC("TN02B2", car), c2 = new VehicleC("TN01A1", bikeT), c3 = new VehicleC("TN07V7", new VehicleType("Van", 45));
        System.out.println(c1.describe() + " fee(130) = " + c1.calculateFee(130));
        System.out.println(c2.describe() + " fee(130) = " + c2.calculateFee(130));
        System.out.println(c3.describe() + " fee(130) = " + c3.calculateFee(130) + "   (new type = new DATA, no new class)");

        // timing of fee computation, both designs
        trace = false;
        Vehicle[] inh = new Vehicle[1000]; VehicleC[] com = new VehicleC[1000];
        for (int i = 0; i < 1000; i++) { inh[i] = (i % 2 == 0) ? new Car("C" + i) : new TwoWheeler("T" + i); com[i] = new VehicleC("V" + i, (i % 2 == 0) ? car : bikeT); }
        int reps = 20_000; double sink = 0;
        for (int w = 0; w < 4; w++) {
            long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (Vehicle v : inh) sink += v.calculateFee(130);
            long t1 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (VehicleC v : com) sink += v.calculateFee(130);
            long t2 = System.nanoTime();
            if (w == 3) System.out.printf("%nFee computation per vehicle (ns): inheritance = %.2f, composition = %.2f (sink=%d)%n",
                    (t1 - t0) / (double) (reps * 1000L), (t2 - t1) / (double) (reps * 1000L), sink > 0 ? 1 : 0);
        }
        System.out.println("Space per vehicle: inheritance = 1 object (regNo ref); composition = 1 object (regNo ref + type ref), VehicleType objects are shared.");
        System.out.println("Both: fee computation O(1).");
    }
}
