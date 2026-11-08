package parking.model;

import java.time.LocalDateTime;

/** Immutable ticket. The number comes from a class-wide counter and is never reused. */
public final class ParkingTicket {
    private static int nextTicketNo = 1000;

    private final int ticketNo;
    private final Vehicle vehicle;
    private final User user;
    private final int floor;
    private final int slot;
    private final LocalDateTime entryTime;

    public ParkingTicket(Vehicle vehicle, User user, int floor, int slot, LocalDateTime entryTime) {
        if (vehicle == null || user == null || entryTime == null) throw new IllegalArgumentException("vehicle, user and entryTime required");
        if (floor < 0 || slot < 0) throw new IllegalArgumentException("floor/slot must be >= 0");
        this.ticketNo = nextTicketNo++;
        this.vehicle = vehicle; this.user = user; this.floor = floor; this.slot = slot; this.entryTime = entryTime;
    }
    public int getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public User getUser() { return user; }
    public int getFloor() { return floor; }
    public int getSlot() { return slot; }
    public LocalDateTime getEntryTime() { return entryTime; }
    @Override public String toString() { return "Ticket#" + ticketNo + "[" + vehicle.getRegNo() + " F" + floor + "-S" + slot + " " + entryTime + "]"; }
}
