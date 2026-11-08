package parking.app;

import parking.model.*;
import parking.service.*;
import java.time.*;

/** Tests for the changed requirements: e-bike, delivery van, one-day pass, 5 floors with different sizes, future types. */
public class Task12Tests {
    static final LocalDate DAY = LocalDate.of(2026, 11, 7);
    static LocalDateTime at(int h, int m) { return DAY.atTime(h, m); }
    static LocalDateTime at(int plusDays, int h, int m) { return DAY.plusDays(plusDays).atTime(h, m); }
    static ParkingService fresh(int... s) { return new ParkingService(new ParkingFacility(s)); }

    public static void main(String[] args) {
        System.out.println("| #  | Category | Test                                           | Expected                                   | Actual                                     | Result |");
        System.out.println("|----|----------|------------------------------------------------|--------------------------------------------|--------------------------------------------|--------|");
        User guest = new Visitor("Guest");
        Vehicle van = VehicleFactory.create("VAN", "TN11V1");
        Vehicle bike = VehicleFactory.create("EBIKE", "TN12E1");

        // E-bike (flat fee)
        TestRunner.check("EBike", "0 minutes is free", "0.0", () -> bike.calculateFee(0));
        TestRunner.check("EBike", "1 minute = flat Rs 15", "15.0", () -> bike.calculateFee(1));
        TestRunner.check("EBike", "10 hours = still Rs 15", "15.0", () -> bike.calculateFee(600));
        TestRunner.check("EBike", "5 days = still Rs 15", "15.0", () -> bike.calculateFee(7200));
        // Delivery van (double car rate 09:00-17:00; an hour is peak if it STARTS in the window)
        TestRunner.check("Van", "10:00-12:00 = 2 peak h x Rs 60", "120.0", () -> van.calculateFee(at(10, 0), at(12, 0)));
        TestRunner.check("Van", "18:00-19:00 = 1 off-peak h x Rs 30", "30.0", () -> van.calculateFee(at(18, 0), at(19, 0)));
        TestRunner.check("Van", "08:30-10:30 = 30 + 60 (straddles 09:00)", "90.0", () -> van.calculateFee(at(8, 30), at(10, 30)));
        TestRunner.check("Van", "16:30-17:30 = hour starts in window -> 60", "60.0", () -> van.calculateFee(at(16, 30), at(17, 30)));
        TestRunner.check("Van", "17:00-18:00 = starts at 17:00 -> off-peak 30", "30.0", () -> van.calculateFee(at(17, 0), at(18, 0)));
        TestRunner.check("Van", "09:00-10:00 (window start is inclusive)", "60.0", () -> van.calculateFee(at(9, 0), at(10, 0)));
        TestRunner.check("Van", "Zero minutes", "0.0", () -> van.calculateFee(at(10, 0), at(10, 0)));
        TestRunner.check("Van", "20:00 -> 08:00 next day = 12 x 30", "360.0", () -> van.calculateFee(at(20, 0), at(1, 8, 0)));
        TestRunner.check("Van", "24 h from 09:00 limited by daily cap", "480.0", () -> van.calculateFee(at(9, 0), at(1, 9, 0)));
        TestRunner.check("Van", "untimed calculateFee(130) uses car rate", "90.0", () -> van.calculateFee(130));
        TestRunner.check("Van", "Exit before entry rejected", "IllegalArgumentException: Exit time is before entry time", () -> van.calculateFee(at(10, 0), at(9, 0)));
        // One-day pass
        TestRunner.check("DayPass", "Visitor with pass for TODAY parks free", "0.0", () -> { ParkingService q = fresh(2); q.enter(new Car("DP01"), new Visitor("V", new DayPass(DAY)), at(9, 0)); return q.exit("DP01", at(14, 0), 0).totalPayable(); });
        TestRunner.check("DayPass", "Pass bought for YESTERDAY does not apply", "150.0", () -> { ParkingService q = fresh(2); q.enter(new Car("DP02"), new Visitor("V", new DayPass(DAY.minusDays(1))), at(9, 0)); return q.exit("DP02", at(14, 0), 0).totalPayable(); });
        TestRunner.check("DayPass", "Pass does not waive charging (EV 10 kWh)", "120.0", () -> { ParkingService q = fresh(2); q.enter(new ElectricCar("DP03"), new Visitor("V", new DayPass(DAY)), at(9, 0)); return q.exit("DP03", at(10, 0), 10).totalPayable(); });
        TestRunner.check("DayPass", "Visitor WITHOUT pass still pays", "30.0", () -> { ParkingService q = fresh(2); q.enter(new Car("DP04"), guest, at(9, 0)); return q.exit("DP04", at(10, 0), 0).totalPayable(); });
        TestRunner.check("DayPass", "Monthly pass (Student) unchanged: free", "0.0", () -> { ParkingService q = fresh(2); q.enter(new Car("DP05"), new Student("A", DAY.plusDays(20)), at(9, 0)); return q.exit("DP05", at(10, 0), 0).totalPayable(); });
        // Facility: 5 floors, different sizes
        int[] cfg = {20, 35, 50, 75, 100};
        TestRunner.check("Floors", "Total slots of 20+35+50+75+100", "280", () -> new ParkingFacility(cfg).totalSlots());
        TestRunner.check("Floors", "21st vehicle goes to floor 1 slot 0", "[1, 0]", () -> { ParkingFacility f = new ParkingFacility(cfg); int[] p = null; for (int i = 0; i < 21; i++) p = f.allocate("V" + i); return "[" + p[0] + ", " + p[1] + "]"; });
        TestRunner.check("Floors", "106th vehicle goes to floor 3 slot 0", "[3, 0]", () -> { ParkingFacility f = new ParkingFacility(cfg); int[] p = null; for (int i = 0; i < 106; i++) p = f.allocate("V" + i); return "[" + p[0] + ", " + p[1] + "]"; });
        TestRunner.check("Floors", "280th vehicle takes the very last slot", "[4, 99]", () -> { ParkingFacility f = new ParkingFacility(cfg); int[] p = null; for (int i = 0; i < 280; i++) p = f.allocate("V" + i); return "[" + p[0] + ", " + p[1] + "]"; });
        TestRunner.check("Floors", "281st vehicle -> full (null)", "null", () -> { ParkingFacility f = new ParkingFacility(cfg); int[] p = null; for (int i = 0; i < 281; i++) p = f.allocate("V" + i); return p == null ? "null" : "not null"; });
        TestRunner.check("Floors", "Release [4, 100] (one past the end)", "false", () -> new ParkingFacility(cfg).release(4, 100));
        // Factory
        TestRunner.check("Factory", "Unknown type still rejected", "IllegalArgumentException: Unknown vehicle type: BUS", () -> VehicleFactory.create("BUS", "TN77X7"));
        TestRunner.check("Factory", "Lower-case 'van' accepted", "DeliveryVan[TN11V1]", () -> VehicleFactory.create("van", "TN11V1").toString());
        // Future type: registered in 3 lines, no existing class edited
        VehicleFactory.register("TRUCK", reg -> new Vehicle(reg) {
            @Override protected double ratePerHour() { return 50; }
            @Override public String category() { return "Truck"; }
        });
        TestRunner.check("Future", "NEW type 'TRUCK' registered at run time", "Truck[TN88T8]", () -> VehicleFactory.create("TRUCK", "TN88T8").toString());
        TestRunner.check("Future", "Truck 130 min = 3 h x Rs 50", "150.0", () -> VehicleFactory.create("TRUCK", "TN88T8").calculateFee(130));
        // Hand-calculated day with the new categories (see input/commands.txt): 120 + 15 + 90 + 0 + 60 = 285
        ParkingService day = fresh(cfg);
        day.enter(VehicleFactory.create("VAN", "TN11V1"), guest, at(10, 0));
        day.enter(VehicleFactory.create("EBIKE", "TN12E1"), guest, at(9, 0));
        day.enter(VehicleFactory.create("VAN", "TN13V2"), guest, at(8, 30));
        day.enter(VehicleFactory.create("CAR", "TN14C1"), new Visitor("V", new DayPass(DAY)), at(9, 0));
        day.enter(VehicleFactory.create("CAR", "TN15C2"), guest, at(9, 0));
        day.exit("TN11V1", at(12, 0), 0);      // 2 peak hours x 60 = 120
        day.exit("TN12E1", at(11, 30), 0);     // flat 15
        day.exit("TN13V2", at(10, 30), 0);     // 30 + 60 = 90
        day.exit("TN14C1", at(14, 0), 0);      // day pass -> 0
        day.exit("TN15C2", at(11, 0), 0);      // 2 h x 30 = 60
        TestRunner.check("Day", "Hand total 120+15+90+0+60", "285.0", () -> day.getCollectedToday());

        System.out.println();
        System.out.println("Result: " + TestRunner.passed + " / " + TestRunner.n + " new tests passed");
        if (TestRunner.passed != TestRunner.n) System.exit(1);
    }
}
