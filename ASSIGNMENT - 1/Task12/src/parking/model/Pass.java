package parking.model;
import java.time.LocalDate;
/** A pass that can exempt its holder from the PARKING fee (never from charging). */
public interface Pass {
    boolean validOn(LocalDate day);
    String name();
}
