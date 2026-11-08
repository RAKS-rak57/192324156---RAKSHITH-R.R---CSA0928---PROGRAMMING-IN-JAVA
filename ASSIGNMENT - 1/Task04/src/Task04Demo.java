import java.time.LocalDateTime;

public class Task04Demo {
    public static void main(String[] args) {
        System.out.println("=== Constructors ===");
        LocalDateTime t = LocalDateTime.of(2026, 11, 7, 9, 30);
        ParkingTicket a = new ParkingTicket("TN01AB1111", 0, 0, t);      // full constructor
        ParkingTicket b = new ParkingTicket("TN02CD2222", 0, 1);         // chains to the full one (time = now)
        ParkingTicket blank = new ParkingTicket();                        // default
        System.out.println("a     = " + a);
        System.out.println("b     = " + b.getRegNo() + " ticketNo " + b.getTicketNo());
        System.out.println("blank = " + blank);

        System.out.println();
        System.out.println("=== Copy constructor (duplicate ticket) vs reference copy ===");
        ParkingTicket dup = new ParkingTicket(a);       // new object on the heap
        ParkingTicket alias = a;                        // second REFERENCE to the same object
        System.out.println("dup   = " + dup);
        System.out.println("dup == a      : " + (dup == a) + "   (different objects)");
        System.out.println("alias == a    : " + (alias == a) + "   (same object, two references)");
        System.out.println("same ticketNo : " + (dup.getTicketNo() == a.getTicketNo()) + "   (number is not consumed again)");
        ParkingTicket c = new ParkingTicket("TN03EF3333", 1, 0, t);
        System.out.println("next new ticket number = " + c.getTicketNo() + "   (a=" + a.getTicketNo() + ", b=" + b.getTicketNo() + ", never reused)");

        System.out.println();
        System.out.println("=== Validation in the constructor ===");
        try { new ParkingTicket("", 0, 0, t); } catch (IllegalArgumentException e) { System.out.println("blank regNo   -> " + e.getMessage()); }
        try { new ParkingTicket("TN09", -3, 0, t); } catch (IllegalArgumentException e) { System.out.println("negative floor -> " + e.getMessage()); }

        System.out.println();
        System.out.println("=== Alternative 1: default constructor + setters ===");
        TicketSetterStyle s = new TicketSetterStyle();
        System.out.println("right after new: isValid() = " + s.isValid() + "  (an INVALID object already exists)");
        s.setRegNo("TN05"); s.setFloor(0); s.setSlot(2); s.setEntryTime(t);
        System.out.println("after 4 setters : isValid() = " + s.isValid());

        System.out.println();
        System.out.println("=== Alternative 2: static factory ===");
        TicketFactory f = TicketFactory.forEntry("TN06", 1, 1, t);
        System.out.println("factory ticket " + f.getTicketNo() + " for " + f.getRegNo());
        try { TicketFactory.forEntry(null, 0, 0, t); } catch (IllegalArgumentException e) { System.out.println("invalid -> " + e.getMessage()); }

        System.out.println();
        System.out.println("=== Cost of creating n tickets (ms) ===");
        int[] ns = {100_000, 1_000_000, 5_000_000};
        System.out.printf("%-10s %-22s %-22s %-22s%n", "n", "constructor chain", "default + 4 setters", "static factory");
        for (int n : ns) {
            long best1 = Long.MAX_VALUE, best2 = Long.MAX_VALUE, best3 = Long.MAX_VALUE; long sink = 0;
            for (int rep = 0; rep < 4; rep++) {
                long x0 = System.nanoTime();
                for (int i = 0; i < n; i++) sink += new ParkingTicket("TN" + (i & 1023), 0, i & 63, t).getTicketNo();
                long x1 = System.nanoTime();
                for (int i = 0; i < n; i++) { TicketSetterStyle q = new TicketSetterStyle(); q.setRegNo("TN" + (i & 1023)); q.setFloor(0); q.setSlot(i & 63); q.setEntryTime(t); sink += q.getTicketNo(); }
                long x2 = System.nanoTime();
                for (int i = 0; i < n; i++) sink += TicketFactory.forEntry("TN" + (i & 1023), 0, i & 63, t).getTicketNo();
                long x3 = System.nanoTime();
                best1 = Math.min(best1, x1 - x0); best2 = Math.min(best2, x2 - x1); best3 = Math.min(best3, x3 - x2);
            }
            System.out.printf("%-10d %-22.1f %-22.1f %-22.1f (sink=%d)%n", n, best1 / 1e6, best2 / 1e6, best3 / 1e6, sink > 0 ? 1 : 0);
        }
        System.out.println("All three create n objects: time O(n), space O(n); only the constant factor differs.");
    }
}
