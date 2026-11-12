package campus.reg.model;

/** One event students can register for. toString() is what the combo box shows. */
public record CampusEvent(int id, String title, String venue, String date) {
    public static final CampusEvent ALL = new CampusEvent(0, "All events", "", "");
    @Override public String toString() { return title; }
}
