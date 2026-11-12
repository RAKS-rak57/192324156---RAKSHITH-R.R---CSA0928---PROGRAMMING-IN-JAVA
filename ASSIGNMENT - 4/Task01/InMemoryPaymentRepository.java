import java.util.ArrayList;
import java.util.List;

/** Used for unit tests and for running the window without a database. */
public class InMemoryPaymentRepository implements PaymentRepository {
    private final List<String[]> rows = new ArrayList<>();     // {id, regNo, amount}
    private long nextId = 1;

    @Override
    public synchronized long save(String regNo, double amount) {
        rows.add(new String[]{String.valueOf(nextId), regNo, String.format("%.2f", amount)});
        return nextId++;
    }

    @Override
    public synchronized List<String> history(String regNo) {
        List<String> lines = new ArrayList<>();
        for (String[] r : rows) {
            if (r[1].equals(regNo)) lines.add(String.format("RCPT-%05d  Rs.%s", Long.parseLong(r[0]), r[2]));
        }
        return lines;
    }

    public synchronized int count() { return rows.size(); }
    public synchronized String lastRegNoStoredLiterally() { return rows.isEmpty() ? null : rows.get(rows.size() - 1)[1]; }
}
