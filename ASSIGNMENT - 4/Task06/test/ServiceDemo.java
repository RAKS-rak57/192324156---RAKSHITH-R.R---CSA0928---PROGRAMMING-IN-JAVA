import campus.reg.dao.InMemoryRegistrationDao;
import campus.reg.model.*;
import campus.reg.service.*;
import java.nio.file.*;
import java.util.List;

/** Console demonstration of the service layer: normal, invalid and duplicate registrations (Stage 5). */
public class ServiceDemo {
    static int pass = 0, fail = 0;
    static void show(String title, ServiceResult r, boolean expectOk) {
        boolean ok = r.success == expectOk;
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-40s -> %s%n", ok ? "PASS" : "FAIL", title, r.message);
    }

    public static void main(String[] args) throws Exception {
        InMemoryRegistrationDao dao = InMemoryRegistrationDao.withDemoData();
        RegistrationService service = new RegistrationService(dao);
        List<CampusEvent> events = service.events();
        System.out.println("Events: " + events);
        CampusEvent tech = events.get(0), cultural = events.get(1), hack = events.get(2);
        System.out.println("Registrations at start: " + service.loadPage(CampusEvent.ALL, 1, 100).totalRows());
        System.out.println();

        System.out.println("--- NORMAL registrations ---");
        show("new student, Hackathon",              service.register("192319001", "Ravi Teja", "ravi@example.com", hack), true);
        show("same student, another event",         service.register("192319001", "Ravi Teja", "ravi@example.com", cultural), true);
        show("name with dot and spaces",            service.register("192319002", "A. P. Mohan", "mohan@college.edu.in", tech), true);

        System.out.println();
        System.out.println("--- INVALID input (nothing reaches the database) ---");
        show("all fields empty",                    service.register("", "", "", tech), false);
        show("register number too short",           service.register("1923", "Ravi Teja", "ravi@example.com", tech), false);
        show("register number with letters",        service.register("19231900A", "Ravi Teja", "ravi@example.com", tech), false);
        show("name with digits",                    service.register("192319003", "Ravi123", "ravi@example.com", tech), false);
        show("e-mail without @",                    service.register("192319003", "Ravi Teja", "ravi.example.com", tech), false);
        show("no event selected (prompt)",          service.register("192319003", "Ravi Teja", "ravi@example.com", CampusEvent.ALL), false);
        show("SQL text in the name",                service.register("192319003", "x'); DROP TABLE student;--", "a@b.com", tech), false);
        show("null values",                         service.register(null, null, null, null), false);

        System.out.println();
        System.out.println("--- DUPLICATE registrations ---");
        show("same student, same event again",      service.register("192319001", "Ravi Teja", "ravi@example.com", hack), false);
        show("same again (different spelling)",     service.register(" 192319001 ", "Ravi T", "ravi@example.com", hack), false);
        show("duplicate of seeded data",            service.register("192311001", "Arun Kumar", "arun@example.com", tech), false);

        // the second line of defence: the UNIQUE key itself (what MySQL does when two clients register at the same moment)
        try {
            dao.register(new Student("192319001", "Ravi Teja", "ravi@example.com"), hack.id());
            System.out.println("FAIL  UNIQUE key did not fire");
            fail++;
        } catch (campus.reg.dao.DuplicateRegistrationException e) {
            System.out.println("PASS direct insert behind the service's back          -> blocked by the UNIQUE key: " + e.getMessage());
            pass++;
        }

        System.out.println();
        System.out.println("--- Paging (page size 5) and filtering ---");
        PageData p = service.loadPage(CampusEvent.ALL, 1, 5);
        System.out.println("All events: " + p.totalRows() + " rows, " + p.pageCount() + " pages; page 1:");
        for (RegistrationRow r : p.rows()) System.out.println("   " + r);
        p = service.loadPage(hack, 1, 5);
        System.out.println("Filter = " + hack + ": " + p.totalRows() + " rows");
        p = service.loadPage(CampusEvent.ALL, 99, 5);
        System.out.println("Page 99 requested -> clamped to page " + p.pageNo() + " of " + p.pageCount());

        System.out.println();
        System.out.println("--- Export ---");
        Path out = Paths.get("registrations_export.csv");
        int n = service.exportCsv(out, CampusEvent.ALL);
        System.out.println(n + " rows written to " + out + ". First lines:");
        List<String> lines = Files.readAllLines(out);
        for (int i = 0; i < Math.min(4, lines.size()); i++) System.out.println("   " + lines.get(i));

        System.out.println();
        System.out.println("Result: " + pass + " passed, " + fail + " failed");
    }
}
