package campus.reg.service;

import campus.reg.dao.*;
import campus.reg.model.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** Business rules: input validation, duplicate check, paging, CSV export. No Swing code here, no SQL here. */
public class RegistrationService {
    private static final Pattern REG_NO = Pattern.compile("\\d{9}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .]{1,59}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");

    private final RegistrationDao dao;

    public RegistrationService(RegistrationDao dao) { this.dao = dao; }

    public List<CampusEvent> events() { return dao.findEvents(); }

    /** Validates the form, checks for a duplicate, saves. Always returns a result; it never throws to the window. */
    public ServiceResult register(String regNoText, String nameText, String emailText, CampusEvent event) {
        String regNo = regNoText == null ? "" : regNoText.trim();
        String name  = nameText == null ? "" : nameText.trim();
        String email = emailText == null ? "" : emailText.trim();

        if (regNo.isEmpty() || name.isEmpty() || email.isEmpty()) return ServiceResult.fail("Register number, name and e-mail are all required.");
        if (!REG_NO.matcher(regNo).matches())  return ServiceResult.fail("Register number must be 9 digits (example 192311001).");
        if (!NAME.matcher(name).matches())     return ServiceResult.fail("Name may contain letters, spaces and dots only (2-60 characters).");
        if (!EMAIL.matcher(email).matches())   return ServiceResult.fail("E-mail address is not valid.");
        if (event == null || event.id() == 0)  return ServiceResult.fail("Select an event.");

        try {
            if (dao.exists(regNo, event.id()))                                  // friendly check first ...
                return ServiceResult.fail(regNo + " is already registered for " + event.title() + ".");
            dao.register(new Student(regNo, name, email), event.id());           // ... and the UNIQUE key is the final guard
            return ServiceResult.ok("Registered " + name + " (" + regNo + ") for " + event.title() + ".");
        } catch (DuplicateRegistrationException e) {
            return ServiceResult.fail(regNo + " is already registered for " + event.title() + ".");
        } catch (DataAccessException e) {
            return ServiceResult.fail("Database problem: " + e.getMessage());
        }
    }

    /** One page of the table (pageNo starts at 1). */
    public PageData loadPage(CampusEvent filter, int pageNo, int pageSize) {
        Integer eventId = (filter == null || filter.id() == 0) ? null : filter.id();
        int total = dao.count(eventId);
        int pages = Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(Math.max(1, pageNo), pages);
        return new PageData(dao.page(eventId, pageSize, (page - 1) * pageSize), total, page, pages);
    }

    /** Writes the (filtered) registrations to a CSV file and returns the number of data rows written. */
    public int exportCsv(Path file, CampusEvent filter) throws IOException {
        Integer eventId = (filter == null || filter.id() == 0) ? null : filter.id();
        List<RegistrationRow> rows = dao.findAll(eventId);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("reg_id,reg_no,student_name,event_title,registered_on");
            w.newLine();
            for (RegistrationRow r : rows) {
                w.write(r.regId() + "," + csv(r.regNo()) + "," + csv(r.studentName()) + "," + csv(r.eventTitle()) + "," + csv(r.registeredOn()));
                w.newLine();
            }
        }
        return rows.size();
    }

    private static String csv(String s) { return (s.contains(",") || s.contains("\"")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }
}
