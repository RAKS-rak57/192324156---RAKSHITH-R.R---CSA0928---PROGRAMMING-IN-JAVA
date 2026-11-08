package parking.model;
import java.time.LocalDate;
/** One-day pass for visitors: valid only on the day it was bought for. */
public final class DayPass implements Pass {
    private final LocalDate validDate;
    public DayPass(LocalDate validDate) {
        if (validDate == null) throw new IllegalArgumentException("validDate required");
        this.validDate = validDate;
    }
    @Override public boolean validOn(LocalDate day) { return validDate.equals(day); }
    @Override public String name() { return "one-day pass " + validDate; }
}
