package parking.service;

import parking.model.ParkingRecord;

/** Different package: can use ONLY the public API of ParkingRecord. */
public class RecordService {
    public ParkingRecord checkout(String regNo, int floor, int slot, int minutes) {
        ParkingRecord r = new ParkingRecord(regNo);
        r.setPosition(floor, slot);
        r.setDurationMinutes(minutes);
        r.bill(30);
        return r;
    }
}
