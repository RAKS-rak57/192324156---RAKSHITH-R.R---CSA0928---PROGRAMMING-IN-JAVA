import java.time.LocalDateTime;

/** Alternative 1: default constructor + setters. The object exists BEFORE it is valid. */
public class TicketSetterStyle {
    private static int next = 1000;
    private int ticketNo; private String regNo; private int floor = -1; private int slot = -1; private LocalDateTime entryTime;
    public TicketSetterStyle() { ticketNo = next++; }
    public void setRegNo(String r) { this.regNo = r; }
    public void setFloor(int f) { this.floor = f; }
    public void setSlot(int s) { this.slot = s; }
    public void setEntryTime(LocalDateTime t) { this.entryTime = t; }
    public boolean isValid() { return regNo != null && floor >= 0 && slot >= 0 && entryTime != null; }
    public int getTicketNo() { return ticketNo; }
    public String getRegNo() { return regNo; }
}
