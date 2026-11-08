import java.lang.ref.WeakReference;
import java.time.LocalDateTime;

public class Task05Demo {

    /** Facility whose counter is an INSTANCE variable (the wrong design for two gates). */
    static class FacilityInstanceCounter {
        private int counter = 1000;
        private final String name;
        FacilityInstanceCounter(String name) { this.name = name; }
        String issue(String reg) { return name + " -> ticket " + (counter++) + " for " + reg; }
    }

    /** Facility using the shared static counter via ParkingTicket. */
    static class FacilityStaticCounter {
        private final String name;
        FacilityStaticCounter(String name) { this.name = name; }
        String issue(String reg) {
            ParkingTicket t = new ParkingTicket(reg, LocalDateTime.of(2026, 11, 7, 9, 0));
            return name + " -> ticket " + t.getTicketNo() + " for " + reg;
        }
    }

    /** Solution 2: one shared issuer object injected into both facilities (no static needed). */
    static class TicketIssuer {
        private int next = 1000;
        int nextNo() { return next++; }
    }
    static class FacilityWithIssuer {
        private final String name; private final TicketIssuer issuer;
        FacilityWithIssuer(String name, TicketIssuer issuer) { this.name = name; this.issuer = issuer; }
        String issue(String reg) { return name + " -> ticket " + issuer.nextNo() + " for " + reg; }
    }

    // ---- object passing --------------------------------------------------
    static void reassign(ParkingTicket t) { t = new ParkingTicket("REASSIGNED", LocalDateTime.now()); }   // changes only the local copy of the reference
    static ParkingTicket makeTicket(String reg) { return new ParkingTicket(reg, LocalDateTime.of(2026, 11, 7, 10, 0)); }  // object RETURNED
    static void tamperArray(String[] a) { a[0] = "CHANGED"; }          // arrays are objects: the caller sees this
    static void tamperInt(int x) { x = 99; }                            // primitives are copied

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Instance counter with two facilities (WRONG) ===");
        FacilityInstanceCounter north = new FacilityInstanceCounter("NORTH"), south = new FacilityInstanceCounter("SOUTH");
        System.out.println(north.issue("TN01AA1111"));
        System.out.println(south.issue("TN02BB2222"));
        System.out.println(north.issue("TN03CC3333"));
        System.out.println(south.issue("TN04DD4444"));
        System.out.println("-> NORTH and SOUTH both issued 1000 and 1001: ticket numbers are reused across the campus.");

        System.out.println();
        System.out.println("=== 2. Static counter (shared by all facilities) ===");
        FacilityStaticCounter n2 = new FacilityStaticCounter("NORTH"), s2 = new FacilityStaticCounter("SOUTH");
        System.out.println(n2.issue("TN01AA1111"));
        System.out.println(s2.issue("TN02BB2222"));
        System.out.println(n2.issue("TN03CC3333"));
        System.out.println(s2.issue("TN04DD4444"));
        System.out.println("tickets issued so far (static getIssuedCount) = " + ParkingTicket.getIssuedCount());

        System.out.println();
        System.out.println("=== 3. Alternative: injected shared TicketIssuer ===");
        TicketIssuer issuer = new TicketIssuer();
        FacilityWithIssuer n3 = new FacilityWithIssuer("NORTH", issuer), s3 = new FacilityWithIssuer("SOUTH", issuer);
        System.out.println(n3.issue("TN01AA1111"));
        System.out.println(s3.issue("TN02BB2222"));
        System.out.println(n3.issue("TN03CC3333"));

        System.out.println();
        System.out.println("=== 4. Static method ===");
        System.out.println("isValidRegNo(\"TN01AB1111\") = " + ParkingTicket.isValidRegNo("TN01AB1111"));
        System.out.println("isValidRegNo(\"hello\")      = " + ParkingTicket.isValidRegNo("hello"));

        System.out.println();
        System.out.println("=== 5. Passing and returning objects ===");
        ParkingTicket t = makeTicket("TN07XY7777");
        System.out.println("returned from method : " + t);
        reassign(t);
        System.out.println("after reassign(t)    : " + t + "   (caller's reference unchanged: Java passes the reference BY VALUE)");
        String[] arr = {"original"};
        tamperArray(arr);
        int x = 5; tamperInt(x);
        System.out.println("array after tamper   : " + arr[0] + "   (the shared object was modified)");
        System.out.println("int after tamperInt  : " + x + "   (primitive copied)");

        System.out.println();
        System.out.println("=== 6. Life cycle: ticket becomes garbage after exit ===");
        ParkingTicket[] active = new ParkingTicket[3];
        active[0] = makeTicket("TN08LIFE0001");
        WeakReference<ParkingTicket> probe = new WeakReference<>(active[0]);
        System.out.println("vehicle inside   : ticket reachable = " + (probe.get() != null));
        active[0] = null;                           // vehicle exits: the only strong reference is removed
        for (int i = 0; i < 5 && probe.get() != null; i++) { System.gc(); Thread.sleep(50); }
        System.out.println("after exit + GC  : ticket collected = " + (probe.get() == null) + "   (eligible once no strong reference remains; collection timing is up to the JVM)");

        System.out.println();
        System.out.println("=== 7. Space used by the counter ===");
        System.out.println("static  counter: 1 int for the whole class, O(1) for k facilities");
        System.out.println("instance counter: 1 int per facility object, O(k)");
        System.out.println("tickets (n): O(n) in both designs");
    }
}
