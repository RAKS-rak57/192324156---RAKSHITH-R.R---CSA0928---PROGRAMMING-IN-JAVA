package campus.reg.dao;

import campus.reg.model.*;
import java.util.*;

/** A DAO that keeps everything in lists. Used for the demo, the tests and the benchmarks (no MySQL needed). */
public class InMemoryRegistrationDao implements RegistrationDao {
    private final List<CampusEvent> events = new ArrayList<>();
    private final Map<String, Student> students = new HashMap<>();
    private final List<Integer> eventOfRow = new ArrayList<>();      // event id of each row (same index as rows)
    private final List<RegistrationRow> rows = new ArrayList<>();
    private final Set<String> keys = new HashSet<>();               // "regNo|eventId" = the UNIQUE (reg_no, event_id) key
    private volatile long latencyMillis;
    private int nextId = 1;

    public InMemoryRegistrationDao(long latencyMillis) { this.latencyMillis = latencyMillis; }
    /** Imitates the network / query time of a real database: every call waits this long. */
    public void setLatency(long millis) { this.latencyMillis = millis; }

    public static InMemoryRegistrationDao withDemoData() {
        InMemoryRegistrationDao d = new InMemoryRegistrationDao(0);
        d.events.add(new CampusEvent(1, "Tech Symposium 2026", "Main Auditorium", "2026-12-05"));
        d.events.add(new CampusEvent(2, "Cultural Night", "Open Air Theatre", "2026-12-12"));
        d.events.add(new CampusEvent(3, "Hackathon 24", "Computer Lab 3", "2027-01-10"));
        String[] names = {"Arun Kumar", "Divya S", "Kavin Raj", "Meena R", "Rahul V", "Sneha P", "Imran K", "Priya N", "Balaji T", "Nisha G", "Anand M", "Latha J"};
        for (int i = 0; i < names.length; i++) {
            String reg = "19231" + String.format("%04d", 1001 + i);
            try { d.register(new Student(reg, names[i], names[i].split(" ")[0].toLowerCase() + "@example.com"), 1 + (i % 3)); }
            catch (DuplicateRegistrationException e) { throw new IllegalStateException(e); }
        }
        return d;
    }

    /** Fast bulk loader for the benchmark (skips the checks). */
    public void bulkAdd(int count) {
        if (events.isEmpty()) events.add(new CampusEvent(1, "Tech Symposium 2026", "Main Auditorium", "2026-12-05"));
        for (int i = 0; i < count; i++) {
            String reg = String.format("%09d", 100000000 + i);
            rows.add(new RegistrationRow(nextId++, reg, "Student " + i, "Tech Symposium 2026", "2026-11-08 10:00:00"));
            eventOfRow.add(1);
        }
    }

    private void pause() { if (latencyMillis > 0) try { Thread.sleep(latencyMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private String title(int eventId) { for (CampusEvent e : events) if (e.id() == eventId) return e.title(); return "?"; }

    @Override public synchronized List<CampusEvent> findEvents() { pause(); return new ArrayList<>(events); }
    @Override public synchronized boolean exists(String regNo, int eventId) { pause(); return keys.contains(regNo + "|" + eventId); }

    @Override
    public synchronized void register(Student s, int eventId) throws DuplicateRegistrationException {
        pause();
        if (!keys.add(s.regNo() + "|" + eventId)) throw new DuplicateRegistrationException("Already registered for this event.");   // the UNIQUE key
        students.put(s.regNo(), s);
        rows.add(new RegistrationRow(nextId++, s.regNo(), s.name(), title(eventId), "2026-11-08 10:" + String.format("%02d", rows.size() % 60) + ":00"));
        eventOfRow.add(eventId);
    }

    private List<RegistrationRow> select(Integer eventId) {
        if (eventId == null) return rows;
        List<RegistrationRow> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) if (eventOfRow.get(i) == eventId.intValue()) out.add(rows.get(i));
        return out;
    }

    @Override public synchronized int count(Integer eventId) { pause(); return select(eventId).size(); }

    @Override
    public synchronized List<RegistrationRow> page(Integer eventId, int limit, int offset) {
        pause();
        List<RegistrationRow> all = select(eventId);
        return new ArrayList<>(all.subList(Math.min(offset, all.size()), Math.min(offset + limit, all.size())));
    }

    @Override public synchronized List<RegistrationRow> findAll(Integer eventId) { pause(); return new ArrayList<>(select(eventId)); }
}
