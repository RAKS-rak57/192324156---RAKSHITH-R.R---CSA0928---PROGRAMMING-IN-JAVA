package parking.app;

import parking.model.*;
import parking.service.*;
import java.time.*;
import java.util.function.Supplier;

/** Hand-written test harness (no external libraries): prints a table of expected vs actual. */
public class TestRunner {
    static int n = 0, passed = 0;
    static final LocalDate DAY = LocalDate.of(2026, 11, 7);
    static LocalDateTime at(int h, int m) { return DAY.atTime(h, m); }

    static void check(String category, String test, Object expected, Supplier<Object> actual) {
        n++;
        Object a;
        try { a = actual.get(); } catch (RuntimeException e) { a = e.getClass().getSimpleName() + ": " + e.getMessage(); }
        boolean ok = String.valueOf(expected).equals(String.valueOf(a));
        if (ok) passed++;
        System.out.printf("| %2d | %-8s | %-46s | %-42s | %-42s | %s |%n", n, category, test, expected, a, ok ? "PASS" : "FAIL");
    }

    static ParkingService fresh(int... slots) { return new ParkingService(new ParkingFacility(slots)); }

    public static void main(String[] args) {
        System.out.println("| #  | Category | Test                                           | Expected                                   | Actual                                     | Result |");
        System.out.println("|----|----------|------------------------------------------------|--------------------------------------------|--------------------------------------------|--------|");

        // ---------------- Normal operation ----------------
        ParkingService s = fresh(2, 1);
        User guest = new Visitor("Guest");
        check("Normal", "TwoWheeler enters -> first slot", "[0, 0]", () -> { ParkingTicket t = s.enter(new TwoWheeler("TN01A1"), guest, at(8, 0)); return "[" + t.getFloor() + ", " + t.getSlot() + "]"; });
        check("Normal", "Car enters -> second slot", "[0, 1]", () -> { ParkingTicket t = s.enter(new Car("TN02B2"), guest, at(8, 0)); return "[" + t.getFloor() + ", " + t.getSlot() + "]"; });
        check("Normal", "Ticket numbers are consecutive", "true", () -> { ParkingService q = fresh(3); int a = q.enter(new Car("AA11"), guest, at(1, 0)).getTicketNo(); int b = q.enter(new Car("BB22"), guest, at(1, 0)).getTicketNo(); return b == a + 1; });
        check("Normal", "TwoWheeler 90 min -> Rs 20", "20.0", () -> s.exit("TN01A1", at(9, 30), 0).totalPayable());
        check("Normal", "Slot freed -> next car gets [0, 0]", "[0, 0]", () -> { ParkingTicket t = s.enter(new Car("TN03C3"), guest, at(9, 40)); return "[" + t.getFloor() + ", " + t.getSlot() + "]"; });
        check("Normal", "Student with valid pass parks free", "0.0", () -> { ParkingService q = fresh(2); q.enter(new Car("ST01"), new Student("Asha", LocalDate.of(2026, 12, 31)), at(8, 0)); return q.exit("ST01", at(12, 0), 0).totalPayable(); });
        check("Normal", "Staff with EXPIRED pass pays 4 h x 30", "120.0", () -> { ParkingService q = fresh(2); q.enter(new Car("SF01"), new Staff("Rao", LocalDate.of(2026, 10, 31)), at(8, 0)); return q.exit("SF01", at(12, 0), 0).totalPayable(); });
        check("Normal", "EV 121 min + 10 kWh = 90 + 120", "210.0", () -> { ParkingService q = fresh(2); q.enter(new ElectricCar("EV01"), guest, at(9, 0)); return q.exit("EV01", at(11, 1), 10).totalPayable(); });
        check("Normal", "Pass holder EV still pays charging", "120.0", () -> { ParkingService q = fresh(2); q.enter(new ElectricCar("EV02"), new Staff("Rao", LocalDate.of(2026, 12, 31)), at(9, 0)); return q.exit("EV02", at(10, 0), 10).totalPayable(); });

        // ---------------- Boundary ----------------
        check("Boundary", "Last free slot is allocated", "[1, 0]", () -> { ParkingService q = fresh(1, 1); q.enter(new Car("BB11"), guest, at(8, 0)); ParkingTicket t = q.enter(new Car("BB22"), guest, at(8, 0)); return "[" + t.getFloor() + ", " + t.getSlot() + "]"; });
        check("Boundary", "Entry when full is refused", "IllegalStateException: Parking facility is full", () -> { ParkingService q = fresh(1); q.enter(new Car("BB11"), guest, at(8, 0)); q.enter(new Car("BB22"), guest, at(8, 0)); return "x"; });
        check("Boundary", "Zero-minute stay costs Rs 0", "0.0", () -> new Car("TN00A0").calculateFee(0));
        check("Boundary", "1 minute = 1 started hour", "30.0", () -> new Car("TN00A0").calculateFee(1));
        check("Boundary", "60 minutes (slab edge) = 1 h", "30.0", () -> new Car("TN00A0").calculateFee(60));
        check("Boundary", "61 minutes (just over) = 2 h", "60.0", () -> new Car("TN00A0").calculateFee(61));
        check("Boundary", "480 min = 8 h = daily cap reached", "240.0", () -> new Car("TN00A0").calculateFee(480));
        check("Boundary", "720 min limited by daily cap", "240.0", () -> new Car("TN00A0").calculateFee(720));
        check("Boundary", "1440 min (exactly one day) = cap", "240.0", () -> new Car("TN00A0").calculateFee(1440));
        check("Boundary", "1441 min = cap + 1 started hour", "270.0", () -> new Car("TN00A0").calculateFee(1441));
        check("Boundary", "Same-minute entry and exit", "0.0", () -> { ParkingService q = fresh(1); q.enter(new Car("Z1Z1"), guest, at(8, 0)); return q.exit("Z1Z1", at(8, 0), 0).totalPayable(); });

        // ---------------- Invalid ----------------
        check("Invalid", "Unknown vehicle type 'BUS'", "IllegalArgumentException: Unknown vehicle type: BUS", () -> VehicleFactory.create("BUS", "TN77X7"));
        check("Invalid", "Duplicate registration number", "IllegalStateException: Duplicate registration number: TN02B2", () -> s.enter(new Car("TN02B2"), guest, at(10, 0)));
        check("Invalid", "Release invalid floor 5", "false", () -> s.getFacility().release(5, 0));
        check("Invalid", "Release negative slot", "false", () -> s.getFacility().release(0, -1));
        check("Invalid", "Release an already-free slot", "false", () -> { ParkingFacility f = new ParkingFacility(new int[]{2}); return f.release(0, 1); });
        check("Invalid", "Negative duration", "IllegalArgumentException: Negative duration: -5", () -> new Car("TN00A0").calculateFee(-5));
        check("Invalid", "Exit of a vehicle that never entered", "IllegalStateException: No active ticket for GHOST1", () -> s.exit("GHOST1", at(11, 0), 0));
        check("Invalid", "Exit before entry", "IllegalArgumentException: Exit time is before entry time", () -> { ParkingService q = fresh(1); q.enter(new Car("T1T1"), guest, at(10, 0)); return q.exit("T1T1", at(9, 0), 0); });
        check("Invalid", "Blank registration number", "IllegalArgumentException: Invalid registration number: ", () -> new Car(""));
        check("Invalid", "Charging a non-EV", "IllegalArgumentException: Car[CAR5] cannot be charged", () -> { ParkingService q = fresh(1); q.enter(new Car("CAR5"), guest, at(8, 0)); return q.exit("CAR5", at(9, 0), 5); });

        // ---------------- Object contract ----------------
        check("Object", "equals: Car vs ElectricCar same regNo", "true", () -> new Car("TN02B2").equals(new ElectricCar("TN02B2")));
        check("Object", "hashCode equal for equal vehicles", "true", () -> new Car("TN02B2").hashCode() == new ElectricCar("TN02B2").hashCode());

        // ---------------- Hand-calculated day ----------------
        ParkingService day = fresh(2, 3, 5);
        day.enter(VehicleFactory.create("TWO", "TN01A1"), new Visitor("V1"), at(8, 0));
        day.enter(VehicleFactory.create("CAR", "TN02B2"), new Student("Asha", LocalDate.of(2026, 12, 31)), at(8, 0));
        day.enter(VehicleFactory.create("CAR", "TN03C3"), new Visitor("V2"), at(10, 0));
        day.enter(VehicleFactory.create("CAR", "TN04D4"), new Visitor("V3"), at(9, 0));
        day.enter(VehicleFactory.create("EV",  "TN05E5"), new Visitor("V4"), at(9, 0));
        day.enter(VehicleFactory.create("CAR", "TN06F6"), new Visitor("V5"), at(0, 0));
        day.exit("TN01A1", at(9, 30), 0);        // 90 min  -> 2 h x 10 = 20
        day.exit("TN02B2", at(17, 0), 0);        // pass    -> 0
        day.exit("TN03C3", at(10, 0), 0);        // 0 min   -> 0
        day.exit("TN04D4", at(10, 0), 0);        // 60 min  -> 30
        day.exit("TN05E5", at(11, 1), 10);       // 121 min -> 3 h x 30 = 90 + 10 kWh x 12 = 120 -> 210
        day.exit("TN06F6", at(12, 0), 0);        // 720 min -> 12 h x 30 = 360, capped at 240
        check("Day", "Hand total 20+0+0+30+210+240", "500.0", () -> day.getCollectedToday());
        check("Day", "All six vehicles have left", "0", () -> day.activeCount());
        check("Day", "All slots free again (10 of 10)", "10", () -> day.getFacility().freeSlots());

        System.out.println();
        System.out.println("Result: " + passed + " / " + n + " tests passed");
        if (passed != n) System.exit(1);
    }
}
