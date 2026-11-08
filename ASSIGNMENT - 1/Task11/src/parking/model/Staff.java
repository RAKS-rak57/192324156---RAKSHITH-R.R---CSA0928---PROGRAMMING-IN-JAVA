package parking.model;
import java.time.LocalDate;
public class Staff extends User {
    private final LocalDate passExpiry;
    public Staff(String name, LocalDate passExpiry) { super(name); this.passExpiry = passExpiry; }
    @Override public boolean isFeeExempt(LocalDate on) { return passExpiry != null && !on.isAfter(passExpiry); }
    @Override public String role() { return "Staff"; }
}
