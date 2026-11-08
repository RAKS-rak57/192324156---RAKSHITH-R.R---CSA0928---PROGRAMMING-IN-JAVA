package parking.model;

/** Result of an exit: everything the cashier needs. */
public record Receipt(int ticketNo, String regNo, int minutes, double parkingFee, boolean exempt, double chargingFee) {
    public double totalPayable() { return (exempt ? 0 : parkingFee) + chargingFee; }
}
