package parking.model;

import java.time.LocalDate;

/** Who is parking. Students and staff may hold a monthly pass; visitors always pay. */
public abstract class User {
    private final String name;
    protected User(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        this.name = name;
    }
    public String getName() { return name; }
    public abstract boolean isFeeExempt(LocalDate on);
    public abstract String role();
}
