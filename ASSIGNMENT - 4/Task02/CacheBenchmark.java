import java.io.PrintWriter;
import java.util.*;

/**
 * Alternative concept: doctors cached once in a Map<String, List<Doctor>>  versus  fetched from the database on every
 * department change.  The "database" is InMemoryAppointmentRepository (a full scan per query, plus an assumed round-trip time).
 */
public class CacheBenchmark {
    static long sink = 0;

    static long usedHeap() {
        for (int i = 0; i < 3; i++) System.gc();
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    static InMemoryAppointmentRepository build(int d, int m, long latency) {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository(latency);
        int perDept = m / d;
        for (int i = 0; i < d; i++)
            for (int j = 0; j < perDept; j++) repo.addDoctor(new Doctor("D" + i + "_" + j, "Dr. Name" + j, "Dept" + i));
        return repo;
    }

    public static void main(String[] args) throws Exception {
        int[][] cases = {{10, 1000}, {10, 100000}, {100, 100000}, {1000, 200000}};
        PrintWriter csv = new PrintWriter("cache_results.csv");
        csv.println("departments,doctors,cache_ns_per_change,db_scan_ns_per_change,db_with_2ms_roundtrip_ms,cache_extra_MB");
        System.out.println("=== Department change: cached Map  vs  query on every change ===");
        System.out.printf("%6s %9s %18s %20s %24s %16s%n", "d", "m", "Map lookup (ns)", "DB query scan (ns)", "DB with 2 ms trip (ms)", "Map memory (MB)");
        for (int[] c : cases) {
            int d = c[0], m = c[1];
            long before = usedHeap();
            InMemoryAppointmentRepository repo = build(d, m, 0);
            long afterRepo = usedHeap();
            AppointmentService service = new AppointmentService(repo);          // builds the Map (the cache)
            long afterCache = usedHeap();
            double cacheMB = (afterCache - afterRepo) / 1048576.0;

            String[] names = new String[d];
            for (int i = 0; i < d; i++) names[i] = "Dept" + i;
            int changes = 20000;
            for (int w = 0; w < 5000; w++) sink += service.doctorsFor(names[w % d]).size();   // warm-up
            long s = System.nanoTime();
            for (int i = 0; i < changes; i++) sink += service.doctorsFor(names[i % d]).size();
            double mapNs = (System.nanoTime() - s) / (double) changes;

            int dbChanges = Math.max(20, 2000000 / m);                           // keep total run time small
            s = System.nanoTime();
            for (int i = 0; i < dbChanges; i++) sink += repo.findDoctors(names[i % d]).size();
            double dbNs = (System.nanoTime() - s) / (double) dbChanges;
            double withTripMs = 2.0 + dbNs / 1e6;
            System.out.printf("%6d %9d %18.1f %20.1f %24.3f %16.2f%n", d, m, mapNs, dbNs, withTripMs, cacheMB);
            csv.printf("%d,%d,%.1f,%.1f,%.3f,%.2f%n", d, m, mapNs, dbNs, withTripMs, cacheMB);
            sink += repo.slots().size();
        }
        csv.close();
        System.out.println("(checksum " + sink + ")");
    }
}
