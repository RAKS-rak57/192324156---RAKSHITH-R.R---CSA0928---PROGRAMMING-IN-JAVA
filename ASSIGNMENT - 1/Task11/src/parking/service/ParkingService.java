package parking.service;

import parking.model.*;
import java.time.Duration;
import java.time.LocalDateTime;

/** Use cases: enter and exit. Active tickets are kept in a plain array (the design evaluated in Task 11). */
public class ParkingService {
    private final ParkingFacility facility;
    private final ParkingTicket[] active;                 // active[0..count-1]
    private int count = 0;
    private double collectedToday = 0;

    public ParkingService(ParkingFacility facility) {
        this.facility = facility;
        this.active = new ParkingTicket[facility.totalSlots()];
    }

    private int indexOf(String regNo) {                   // linear search: O(n)
        for (int i = 0; i < count; i++) if (active[i].getVehicle().getRegNo().equals(regNo)) return i;
        return -1;
    }

    public ParkingTicket enter(Vehicle v, User u, LocalDateTime when) {
        if (indexOf(v.getRegNo()) >= 0) throw new IllegalStateException("Duplicate registration number: " + v.getRegNo());
        int[] pos = facility.allocate(v.getRegNo());
        if (pos == null) throw new IllegalStateException("Parking facility is full");
        ParkingTicket t = new ParkingTicket(v, u, pos[0], pos[1], when);
        active[count++] = t;
        return t;
    }

    public Receipt exit(String regNo, LocalDateTime when, int kWh) {
        int i = indexOf(regNo);
        if (i < 0) throw new IllegalStateException("No active ticket for " + regNo);
        ParkingTicket t = active[i];
        if (when.isBefore(t.getEntryTime())) throw new IllegalArgumentException("Exit time is before entry time");
        int minutes = (int) Duration.between(t.getEntryTime(), when).toMinutes();
        Vehicle v = t.getVehicle();
        double parking = v.calculateFee(minutes);
        boolean exempt = t.getUser().isFeeExempt(t.getEntryTime().toLocalDate());
        double charging = 0;
        if (kWh > 0) {
            if (!(v instanceof Chargeable)) throw new IllegalArgumentException(v + " cannot be charged");
            charging = ((Chargeable) v).chargingFee(kWh);
        }
        facility.release(t.getFloor(), t.getSlot());
        active[i] = active[--count];                      // remove by moving the last ticket into the hole
        active[count] = null;                             // drop the reference -> the ticket can be garbage collected
        Receipt r = new Receipt(t.getTicketNo(), regNo, minutes, parking, exempt, charging);
        collectedToday += r.totalPayable();
        return r;
    }

    public int activeCount() { return count; }
    public double getCollectedToday() { return collectedToday; }
    public ParkingFacility getFacility() { return facility; }
}
