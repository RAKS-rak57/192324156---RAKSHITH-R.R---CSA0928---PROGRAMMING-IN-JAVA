package campus.reg.dao;

import campus.reg.model.*;
import java.util.List;

/** Everything the application needs from the database. The service layer only knows this interface. */
public interface RegistrationDao {
    List<CampusEvent> findEvents();
    boolean exists(String regNo, int eventId);
    void register(Student student, int eventId) throws DuplicateRegistrationException;
    /** eventId null = all events. */
    int count(Integer eventId);
    /** One page: SQL LIMIT limit OFFSET offset. */
    List<RegistrationRow> page(Integer eventId, int limit, int offset);
    /** Every row of the selection (used by the CSV export). */
    List<RegistrationRow> findAll(Integer eventId);
}
