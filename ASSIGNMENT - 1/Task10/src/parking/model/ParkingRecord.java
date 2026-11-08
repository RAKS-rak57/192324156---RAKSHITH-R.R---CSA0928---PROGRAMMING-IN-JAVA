package parking.model;

/** Encapsulated record: private state, validating setters, controlled access. */
public class ParkingRecord {
    public static final int MAX_FLOORS = 5;                 // public constant: part of the public contract
    private static final int MAX_MINUTES = 7 * 24 * 60;     // private constant: internal rule

    private String regNo;                                    // private: only this class can touch the fields
    private int floor = -1;
    private int slot = -1;
    private int durationMinutes;
    private double fee;

    public ParkingRecord(String regNo) { setRegNo(regNo); }

    // ---------------- public API (validating getters/setters) ----------------
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) {
        if (!isValidRegNo(regNo)) throw new IllegalArgumentException("Invalid registration number: " + regNo);
        this.regNo = regNo;
    }
    public int getFloor() { return floor; }
    public int getSlot() { return slot; }
    public void setPosition(int floor, int slot) {          // floor and slot are validated TOGETHER
        if (floor < 0 || floor >= MAX_FLOORS) throw new IllegalArgumentException("Invalid floor: " + floor);
        if (slot < 0) throw new IllegalArgumentException("Invalid slot: " + slot);
        this.floor = floor; this.slot = slot;
    }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Duration cannot be negative: " + minutes);
        if (minutes > MAX_MINUTES) throw new IllegalArgumentException("Duration exceeds 7 days: " + minutes);
        this.durationMinutes = minutes;
    }
    public double getFee() { return fee; }                   // no setFee(): the fee can only be produced by billing

    public void bill(double ratePerHour) {
        if (ratePerHour <= 0) throw new IllegalArgumentException("rate must be positive");
        this.fee = ((durationMinutes + 59) / 60) * ratePerHour;
    }

    // ---------------- non-public members ----------------
    protected double ratePerHourOrDefault() { return 30; }  // protected: subclasses (e.g. EV record) may override it
    static boolean isValidRegNo(String r) {                  // default (package-private): helper for classes in parking.model only
        return r != null && r.matches("[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{1,4}");
    }
    @Override public String toString() { return "ParkingRecord[" + regNo + ", F" + floor + "-S" + slot + ", " + durationMinutes + " min, Rs " + fee + "]"; }
}
