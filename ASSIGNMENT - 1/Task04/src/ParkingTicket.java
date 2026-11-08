import java.time.LocalDateTime;

/** Task 4 - ParkingTicket: overloaded constructors, this(), constructor chaining, copy constructor. */
public class ParkingTicket {
    private static int nextTicketNo = 1000;           // class-wide counter (explained in Task 5)

    private final int ticketNo;
    private final String regNo;
    private final int floor;
    private final int slot;
    private final LocalDateTime entryTime;
    private final boolean duplicate;                  // true only for re-issued copies

    /** Default constructor: an UNASSIGNED placeholder ticket (used to pre-fill pools). */
    public ParkingTicket() {
        this("UNASSIGNED", -1, -1, LocalDateTime.of(2000, 1, 1, 0, 0), false);
    }

    /** Parameterised constructor: entry time is "now". Chains to the full constructor. */
    public ParkingTicket(String regNo, int floor, int slot) {
        this(regNo, floor, slot, LocalDateTime.now());
    }

    /** Full constructor with validation - the ONLY place where a normal ticket is built. */
    public ParkingTicket(String regNo, int floor, int slot, LocalDateTime entryTime) {
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("regNo required");
        if (floor < 0 || slot < 0) throw new IllegalArgumentException("floor/slot must be >= 0");
        if (entryTime == null) throw new IllegalArgumentException("entryTime required");
        this.ticketNo = nextTicketNo++;
        this.regNo = regNo;
        this.floor = floor;
        this.slot = slot;
        this.entryTime = entryTime;
        this.duplicate = false;
    }

    /** Private constructor used by the default constructor (skips validation and numbering). */
    private ParkingTicket(String regNo, int floor, int slot, LocalDateTime entryTime, boolean duplicate) {
        this.ticketNo = -1;
        this.regNo = regNo;
        this.floor = floor;
        this.slot = slot;
        this.entryTime = entryTime;
        this.duplicate = duplicate;
    }

    /** Copy constructor: re-issue a DUPLICATE ticket - same number, flagged as duplicate, independent object. */
    public ParkingTicket(ParkingTicket other) {
        this.ticketNo = other.ticketNo;               // the ticket number is NOT consumed again
        this.regNo = other.regNo;
        this.floor = other.floor;
        this.slot = other.slot;
        this.entryTime = other.entryTime;             // LocalDateTime is immutable, safe to share
        this.duplicate = true;
    }

    public int getTicketNo() { return ticketNo; }
    public String getRegNo() { return regNo; }
    public int getFloor() { return floor; }
    public int getSlot() { return slot; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public boolean isDuplicate() { return duplicate; }

    @Override public String toString() {
        return "Ticket#" + ticketNo + "[" + regNo + " F" + floor + "-S" + slot + " " + entryTime + (duplicate ? " DUPLICATE" : "") + "]";
    }
}
