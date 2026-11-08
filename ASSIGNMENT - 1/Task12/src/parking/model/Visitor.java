package parking.model;
import java.time.LocalDate;
public class Visitor extends User {
    private final Pass pass;                              // null = pays the full fee
    public Visitor(String name) { this(name, null); }
    public Visitor(String name, Pass pass) { super(name); this.pass = pass; }
    @Override public boolean isFeeExempt(LocalDate on) { return pass != null && pass.validOn(on); }
    @Override public String role() { return pass == null ? "Visitor" : "Visitor (" + pass.name() + ")"; }
}
