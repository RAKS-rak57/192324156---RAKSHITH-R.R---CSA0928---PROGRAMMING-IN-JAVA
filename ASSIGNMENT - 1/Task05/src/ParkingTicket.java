import java.time.LocalDateTime;

/** Task 5 - static members: the ticket counter lives in the CLASS, not in any one ticket. */
public class ParkingTicket {
    private static int nextTicketNo;                  // ONE copy shared by every ticket
    private static int issuedCount;                   // how many tickets exist overall
    static {                                          // static block: runs ONCE when the class is loaded
        nextTicketNo = 1000;
        issuedCount = 0;
        System.out.println("[static block] ParkingTicket class loaded, counter starts at " + nextTicketNo);
    }

    private final int ticketNo;
    private final String regNo;
    private final LocalDateTime entryTime;

    public ParkingTicket(String regNo, LocalDateTime entryTime) {
        this.ticketNo = nextTicketNo++;               // never reused
        this.regNo = regNo;
        this.entryTime = entryTime;
        issuedCount++;
    }

    public static int getIssuedCount() { return issuedCount; }     // static method: needs no object
    public static boolean isValidRegNo(String r) {                 // utility behaviour of the class as a whole
        return r != null && r.matches("[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}");
    }
    public int getTicketNo() { return ticketNo; }
    public String getRegNo() { return regNo; }
    public LocalDateTime getEntryTime() { return entryTime; }
    @Override public String toString() { return "Ticket#" + ticketNo + "[" + regNo + "]"; }
}
