package parking.app;
import parking.model.ParkingRecord;
public class AccessViolations {
    void f(ParkingRecord r) {
        r.durationMinutes = -5;                 // private field
        boolean ok = ParkingRecord.isValidRegNo("TN01A1");   // package-private method
        double d = r.ratePerHourOrDefault();    // protected, different package, not a subclass
    }
}
