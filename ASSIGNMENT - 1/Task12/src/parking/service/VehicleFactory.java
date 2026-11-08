package parking.service;

import parking.model.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Creates vehicles from gate text. New categories are REGISTERED here - no other class has to change. */
public final class VehicleFactory {
    private static final Map<String, Function<String, Vehicle>> REGISTRY = new LinkedHashMap<>();
    static {
        register("TWO", TwoWheeler::new);   register("TWOWHEELER", TwoWheeler::new);
        register("CAR", Car::new);
        register("EV", ElectricCar::new);   register("ELECTRICCAR", ElectricCar::new);
        register("EBIKE", EBike::new);
        register("VAN", DeliveryVan::new);
    }
    private VehicleFactory() { }
    public static void register(String type, Function<String, Vehicle> maker) { REGISTRY.put(type.toUpperCase(), maker); }
    public static Vehicle create(String type, String regNo) {
        Function<String, Vehicle> maker = type == null ? null : REGISTRY.get(type.toUpperCase());
        if (maker == null) throw new IllegalArgumentException("Unknown vehicle type: " + type);
        return maker.apply(regNo);
    }
}
