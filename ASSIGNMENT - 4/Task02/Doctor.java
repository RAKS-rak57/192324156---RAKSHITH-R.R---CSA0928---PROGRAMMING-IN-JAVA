/** A doctor shown in the doctor list. toString() is what the JComboBox displays. */
public record Doctor(String id, String name, String department) {
    @Override public String toString() { return name; }
}
