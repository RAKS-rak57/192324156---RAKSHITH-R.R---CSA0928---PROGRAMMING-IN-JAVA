package parking.service;

/** Jagged array: floor f has its own number of slots. slots[f][s] holds a registration number or null. */
public class ParkingFacility {
    private final String[][] slots;

    public ParkingFacility(int[] slotsPerFloor) {
        if (slotsPerFloor == null || slotsPerFloor.length == 0) throw new IllegalArgumentException("at least one floor");
        slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            if (slotsPerFloor[f] <= 0) throw new IllegalArgumentException("floor " + f + " needs >= 1 slot");
            slots[f] = new String[slotsPerFloor[f]];
        }
    }
    /** Returns {floor, slot} of the first free slot, or null if the facility is full. */
    public int[] allocate(String regNo) {
        for (int f = 0; f < slots.length; f++)
            for (int s = 0; s < slots[f].length; s++)
                if (slots[f][s] == null) { slots[f][s] = regNo; return new int[]{f, s}; }
        return null;
    }
    /** Frees a slot. False if the position is invalid or already free. */
    public boolean release(int floor, int slot) {
        if (floor < 0 || floor >= slots.length) return false;
        if (slot < 0 || slot >= slots[floor].length) return false;
        if (slots[floor][slot] == null) return false;
        slots[floor][slot] = null;
        return true;
    }
    public int totalSlots() { int t = 0; for (String[] f : slots) t += f.length; return t; }
    public int freeSlots() { int t = 0; for (String[] f : slots) for (String s : f) if (s == null) t++; return t; }
    public int floors() { return slots.length; }
}
