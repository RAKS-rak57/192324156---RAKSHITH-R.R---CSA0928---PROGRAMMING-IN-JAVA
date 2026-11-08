package parking.model;

/** Capability of vehicles that can be charged (EVs). */
public interface Chargeable {
    double RATE_PER_KWH = 12;
    default double chargingFee(int kWh) {
        if (kWh < 0) throw new IllegalArgumentException("kWh cannot be negative");
        return kWh * RATE_PER_KWH;
    }
}
