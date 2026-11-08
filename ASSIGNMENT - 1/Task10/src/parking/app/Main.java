package parking.app;

import parking.model.*;
import parking.service.RecordService;

public class Main {
    static void attempt(String label, Runnable r) {
        try { r.run(); System.out.println(label + " -> accepted"); }
        catch (IllegalArgumentException e) { System.out.println(label + " -> REJECTED: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        System.out.println("=== Package structure ===");
        System.out.println("parking.model   : ParkingRecord, EvParkingRecord, PublicRecord (data + rules)");
        System.out.println("parking.service : RecordService (use cases)");
        System.out.println("parking.app     : Main (entry point)");

        System.out.println();
        System.out.println("=== Access modifiers used ===");
        System.out.printf("%-34s %-10s %s%n", "member", "modifier", "reason");
        System.out.printf("%-34s %-10s %s%n", "ParkingRecord.regNo/floor/slot...", "private", "state can change only through validating methods");
        System.out.printf("%-34s %-10s %s%n", "getters / setters / bill()", "public", "the API other packages may call");
        System.out.printf("%-34s %-10s %s%n", "ratePerHourOrDefault()", "protected", "overridable by subclasses only");
        System.out.printf("%-34s %-10s %s%n", "isValidRegNo()", "(default)", "helper visible inside parking.model only");
        System.out.printf("%-34s %-10s %s%n", "MAX_MINUTES", "private", "internal rule, not part of the contract");
        System.out.printf("%-34s %-10s %s%n", "MAX_FLOORS", "public", "constant other classes may read");

        System.out.println();
        System.out.println("=== Validating setters protect the object ===");
        ParkingRecord r = new ParkingRecord("TN01AB1111");
        attempt("setDurationMinutes(-5)   ", () -> r.setDurationMinutes(-5));
        attempt("setDurationMinutes(130)  ", () -> r.setDurationMinutes(130));
        attempt("setPosition(9, 3)        ", () -> r.setPosition(9, 3));
        attempt("setPosition(2, 14)       ", () -> r.setPosition(2, 14));
        attempt("setRegNo(\"hello\")        ", () -> r.setRegNo("hello"));
        r.bill(30);
        System.out.println("state after the valid calls: " + r);

        System.out.println();
        System.out.println("=== Service in another package uses only the public API ===");
        System.out.println(new RecordService().checkout("TN02CD2222", 1, 4, 61));
        EvParkingRecord ev = new EvParkingRecord("TN03EV3333");
        ev.setDurationMinutes(130); ev.billStandard();
        System.out.println(ev);

        System.out.println();
        System.out.println("=== Wrapper classes and autoboxing ===");
        Integer a = 127, b = 127, c = 1000, d = 1000;
        System.out.println("Integer 127  == 127  : " + (a == b) + "   (small values come from the Integer cache)");
        System.out.println("Integer 1000 == 1000 : " + (c == d) + "  (two different objects -> == compares references!)");
        System.out.println("Integer 1000 equals  : " + c.equals(d) + "   (always use equals() or intValue() for wrapper values)");
        Integer nullable = null;
        try { int x = nullable; System.out.println(x); } catch (NullPointerException e) { System.out.println("unboxing null Integer -> NullPointerException (e.g. a map.get() for a missing vehicle)"); }
        java.util.Map<String, Integer> minutesByReg = new java.util.HashMap<>();
        minutesByReg.put("TN01AB1111", 45);                  // autoboxing int -> Integer
        int m = minutesByReg.get("TN01AB1111");              // auto-unboxing Integer -> int
        System.out.println("autoboxing in a Map<String,Integer>: " + m + " minutes; cost: each Integer is an object (~16 bytes) instead of a 4-byte int");

        System.out.println();
        System.out.println("=== Alternative: public fields, caller validates ===");
        PublicRecord p = new PublicRecord();
        p.regNo = "hello"; p.durationMinutes = -500; p.floor = 99; p.fee = -1;
        System.out.println("PublicRecord accepted: regNo=" + p.regNo + " duration=" + p.durationMinutes + " floor=" + p.floor + " fee=" + p.fee + "   <- invalid state, nothing stopped it");
        System.out.println("The same duration check would have to be copied into EVERY caller that sets durationMinutes:");
        System.out.println("  entry screen, exit screen, import tool, report fixer ... = n copies. Encapsulated: 1 copy (setDurationMinutes).");
        System.out.println("Validation cost per update: O(1) in both designs (a couple of comparisons); only the number of places differs.");
    }
}
