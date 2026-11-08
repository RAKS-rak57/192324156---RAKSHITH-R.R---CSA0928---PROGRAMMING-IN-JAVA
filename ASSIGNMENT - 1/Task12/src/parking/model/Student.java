package parking.model;
import java.time.LocalDate;
public class Student extends User {
    private final LocalDate passExpiry;                   // null = no monthly pass
    public Student(String name, LocalDate passExpiry) { super(name); this.passExpiry = passExpiry; }
    @Override public boolean isFeeExempt(LocalDate on) { return passExpiry != null && !on.isAfter(passExpiry); }
    @Override public String role() { return "Student"; }
}
