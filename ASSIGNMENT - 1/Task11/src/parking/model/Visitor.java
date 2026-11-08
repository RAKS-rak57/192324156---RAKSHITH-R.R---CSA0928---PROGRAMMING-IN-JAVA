package parking.model;
import java.time.LocalDate;
public class Visitor extends User {
    public Visitor(String name) { super(name); }
    @Override public boolean isFeeExempt(LocalDate on) { return false; }
    @Override public String role() { return "Visitor"; }
}
