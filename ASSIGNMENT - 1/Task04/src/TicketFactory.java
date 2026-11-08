import java.time.LocalDateTime;

/** Alternative 2: static factory methods with private constructor. */
public final class TicketFactory {
    private static int next = 1000;
    private final int ticketNo; private final String regNo; private final int floor; private final int slot; private final LocalDateTime entryTime;
    private TicketFactory(String r, int f, int s, LocalDateTime t) {
        this.ticketNo = next++; this.regNo = r; this.floor = f; this.slot = s; this.entryTime = t;
    }
    public static TicketFactory forEntry(String r, int f, int s, LocalDateTime t) {
        if (r == null || r.isBlank() || f < 0 || s < 0 || t == null) throw new IllegalArgumentException("invalid ticket");
        return new TicketFactory(r, f, s, t);
    }
    public static TicketFactory forEntryNow(String r, int f, int s) { return forEntry(r, f, s, LocalDateTime.now()); }
    public int getTicketNo() { return ticketNo; }
    public String getRegNo() { return regNo; }
}
