package poly;

import java.time.LocalTime;
import java.time.Duration;

/** Task 8 - overloading (compile time), upcasting, dynamic dispatch trace, instanceof alternative. */
public class PolymorphismDemo {
    static boolean trace = false;

    static abstract class Vehicle {
        private final String regNo;
        Vehicle(String r) { regNo = r; }
        String getRegNo() { return regNo; }
        protected abstract double ratePerHour();

        /** Overload 1 - chosen when the argument is an int. */
        public double calculateFee(int minutes) {
            return ((minutes + 59) / 60) * ratePerHour();
        }
        /** Overload 2 - chosen when the arguments are two LocalTime values (an overnight stay wraps past midnight). */
        public double calculateFee(LocalTime in, LocalTime out) {
            long minutes = Duration.between(in, out).toMinutes();
            if (minutes < 0) minutes += 24 * 60;
            return calculateFee((int) minutes);          // reuses overload 1 (a call chosen at compile time)
        }
    }
    static class TwoWheeler extends Vehicle {
        TwoWheeler(String r) { super(r); }
        @Override protected double ratePerHour() { if (trace) System.out.println("      [executing] TwoWheeler.ratePerHour()"); return 10; }
    }
    static class Car extends Vehicle {
        Car(String r) { super(r); }
        @Override protected double ratePerHour() { if (trace) System.out.println("      [executing] Car.ratePerHour()"); return 30; }
    }
    static class ElectricCar extends Car {
        ElectricCar(String r) { super(r); }              // inherits Car.ratePerHour(): no override needed
    }

    /** The alternative: instanceof + casting chain. */
    static double feeWithInstanceof(Vehicle v, int minutes) {
        int hours = (minutes + 59) / 60;
        if (v instanceof TwoWheeler) return hours * 10;
        else if (v instanceof ElectricCar) return hours * 30;   // must test the subclass BEFORE its parent
        else if (v instanceof Car) return hours * 30;
        throw new IllegalArgumentException("unknown type");
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Compile-time polymorphism: overloaded calculateFee ===");
        Vehicle car = new Car("TN02B2");
        System.out.println("calculateFee(130)                              -> int version      = " + car.calculateFee(130));
        System.out.println("calculateFee(LocalTime 09:00, LocalTime 11:10) -> LocalTime version = " + car.calculateFee(LocalTime.of(9, 0), LocalTime.of(11, 10)));
        System.out.println("calculateFee(LocalTime 22:00, LocalTime 01:00) -> overnight         = " + car.calculateFee(LocalTime.of(22, 0), LocalTime.of(1, 0)));
        System.out.println("The compiler picks the overload from the NUMBER and STATIC TYPES of the arguments, before the program runs.");
        byte b = 5;
        System.out.println("calculateFee((byte) 5) widens byte -> int and picks the int version: " + car.calculateFee(b));

        System.out.println();
        System.out.println("=== 2. Upcasting: one array, many types ===");
        Vehicle[] lot = { new TwoWheeler("TN01A1"), new Car("TN02B2"), new ElectricCar("TN03C3") };
        for (Vehicle v : lot) System.out.println("declared type Vehicle, actual type " + v.getClass().getSimpleName());

        System.out.println();
        System.out.println("=== 3. Dynamic method dispatch trace ===");
        trace = true;
        for (Vehicle v : lot) {
            System.out.println("call: v.calculateFee(130)   declared type = Vehicle, actual type = " + v.getClass().getSimpleName());
            System.out.println("   calculateFee(int) is not overridden -> Vehicle.calculateFee runs, and inside it:");
            System.out.println("      this.ratePerHour() is resolved at RUN TIME from the actual object:");
            double f = v.calculateFee(130);
            System.out.println("   result = Rs " + f);
        }
        trace = false;

        System.out.println();
        System.out.println("=== 4. Alternative: instanceof chain (results must match) ===");
        for (Vehicle v : lot) System.out.println(v.getClass().getSimpleName() + ": dispatch = " + v.calculateFee(130) + ", instanceof = " + feeWithInstanceof(v, 130));

        int n = 3000; Vehicle[] many = new Vehicle[n];
        for (int i = 0; i < n; i++) many[i] = (i % 3 == 0) ? new TwoWheeler("T" + i) : (i % 3 == 1) ? new ElectricCar("E" + i) : new Car("C" + i);
        double sink = 0; int reps = 10_000;
        for (int w = 0; w < 4; w++) {
            long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (Vehicle v : many) sink += v.calculateFee(130);
            long t1 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (Vehicle v : many) sink += feeWithInstanceof(v, 130);
            long t2 = System.nanoTime();
            if (w == 3) System.out.printf("time per call (ns): dynamic dispatch = %.2f, instanceof chain = %.2f (sink=%d)%n",
                    (t1 - t0) / (double) ((long) reps * n), (t2 - t1) / (double) ((long) reps * n), sink > 0 ? 1 : 0);
        }
        System.out.println("dispatch: O(1) for any k (one vtable lookup);  instanceof chain: up to k type tests = O(k), and the chain must be edited for every new type.");
    }
}
