package parking.service;

import parking.model.*;

/** Creates a vehicle from the text typed at the gate. Unknown types are rejected. */
public final class VehicleFactory {
    private VehicleFactory() { }
    public static Vehicle create(String type, String regNo) {
        if (type == null) throw new IllegalArgumentException("Unknown vehicle type: null");
        switch (type.toUpperCase()) {
            case "TWO": case "TWOWHEELER": return new TwoWheeler(regNo);
            case "CAR":                    return new Car(regNo);
            case "EV": case "ELECTRICCAR": return new ElectricCar(regNo);
            default: throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}
