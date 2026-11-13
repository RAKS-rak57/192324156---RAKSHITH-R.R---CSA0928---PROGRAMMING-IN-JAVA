import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Task B1.4 and the alternatives of A1 / B1: find a pass as the file grows; HashMap loaded once; fixed-length records. */
public class GrowthBenchmark {
    static long sink = 0;
    static BusPass make(int i) { return new BusPass("P" + (1001 + i), String.format("19%07d", i), "Student Name " + (i % 1000), 1 + i % 40, "Stop " + (i % 25), LocalDate.of(2027, 6, 30), 3000 + i % 2000); }
    static long used() { for (int i = 0; i < 3; i++) System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    public static void main(String[] args) throws Exception {
        int[] ns = {1000, 5000, 10000, 50000, 100000};
        PrintWriter csv = new PrintWriter("growth_results.csv");
        csv.println("n,text_bytes,fixed_bytes,scan_last_ms,scan_middle_ms,scan_missing_ms,fixed_seek_us,map_load_ms,map_lookup_ns,map_heap_MB");
        System.out.println("=== B1.4 Time to find ONE pass as the file grows (average of repeated runs) ===");
        System.out.printf("%8s %12s %12s %14s %16s %14s %14s%n", "n", "text bytes", "fixed bytes", "scan: last (ms)", "scan: middle (ms)", "scan: missing", "fixed seek (us)");
        // warm-up
        { Path w = Files.createTempFile("w", ".txt"); try (BufferedWriter bw = Files.newBufferedWriter(w)) { for (int i = 0; i < 5000; i++) bw.write(make(i).toLine() + "\n"); }
          BusPassStore s = new BusPassStore(w); for (int i = 0; i < 30; i++) sink += s.find("P6000") == null ? 1 : 0; Files.delete(w); }
        for (int n : ns) {
            Path text = Files.createTempFile("passes" + n, ".txt");
            Path fixed = Files.createTempFile("fixed" + n, ".dat");
            try (BufferedWriter bw = Files.newBufferedWriter(text)) { for (int i = 0; i < n; i++) bw.write(make(i).toLine() + "\n"); }
            try (FixedRecordStore fs = new FixedRecordStore(fixed)) { for (int i = 0; i < n; i++) fs.put(make(i)); }
            BusPassStore store = new BusPassStore(text);
            int reps = n >= 50000 ? 10 : 30;
            double last = 0, mid = 0, miss = 0;
            for (int r = 0; r < reps; r++) {
                long t = System.nanoTime(); sink += store.find("P" + (1001 + n - 1)) != null ? 1 : 0; last += (System.nanoTime() - t) / 1e6;
                t = System.nanoTime(); sink += store.find("P" + (1001 + n / 2)) != null ? 1 : 0; mid += (System.nanoTime() - t) / 1e6;
                t = System.nanoTime(); sink += store.find("P0") != null ? 1 : 0; miss += (System.nanoTime() - t) / 1e6;
            }
            last /= reps; mid /= reps; miss /= reps;

            double seekUs;
            try (FixedRecordStore fs = new FixedRecordStore(fixed)) {
                for (int i = 0; i < 2000; i++) sink += fs.find("P" + (1001 + (i * 7919) % n)).route;       // warm-up
                int q = 20000; long t = System.nanoTime();
                for (int i = 0; i < q; i++) sink += fs.find("P" + (1001 + (i * 7919) % n)).route;
                seekUs = (System.nanoTime() - t) / 1e3 / q;
            }

            // HashMap alternative: read the file ONCE, then every search is a hash lookup
            long before = used();
            long t0 = System.nanoTime();
            Map<String, BusPass> map = new HashMap<>();
            store.scan(p -> { map.put(p.passId, p); return true; });
            double loadMs = (System.nanoTime() - t0) / 1e6;
            double heapMB = (used() - before) / 1048576.0;
            int q = 200000; t0 = System.nanoTime();
            for (int i = 0; i < q; i++) sink += map.get("P" + (1001 + (i * 7919) % n)).route;
            double lookupNs = (System.nanoTime() - t0) / (double) q;
            sink += map.size();

            long tb = Files.size(text), fb = Files.size(fixed);
            System.out.printf("%,8d %,12d %,12d %14.2f %16.2f %14.2f %14.2f%n", n, tb, fb, last, mid, miss, seekUs);
            csv.printf("%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.1f,%.2f%n", n, tb, fb, last, mid, miss, seekUs, loadMs, lookupNs, heapMB);
            Files.delete(text); Files.delete(fixed);
        }
        csv.close();

        // ---------------- q searches on n passes: scan every time  vs  HashMap loaded once ----------------
        System.out.println();
        System.out.println("=== q searches on n passes: O(q * n) scan  versus  O(n + q) HashMap ===");
        System.out.printf("%8s %6s %18s %26s %18s%n", "n", "q", "scan every time (ms)", "HashMap: load + q lookups (ms)", "speed-up");
        PrintWriter c2 = new PrintWriter("search_many_results.csv");
        c2.println("n,q,scan_total_ms,hashmap_total_ms");
        for (int n : new int[]{1000, 10000, 100000}) {
            int q = 100;
            Path text = Files.createTempFile("many" + n, ".txt");
            try (BufferedWriter bw = Files.newBufferedWriter(text)) { for (int i = 0; i < n; i++) bw.write(make(i).toLine() + "\n"); }
            BusPassStore store = new BusPassStore(text);
            long t0 = System.nanoTime();
            for (int i = 0; i < q; i++) sink += store.find("P" + (1001 + (i * 7919) % n)) != null ? 1 : 0;
            double scan = (System.nanoTime() - t0) / 1e6;
            t0 = System.nanoTime();
            Map<String, BusPass> map = new HashMap<>();
            store.scan(p -> { map.put(p.passId, p); return true; });
            for (int i = 0; i < q; i++) sink += map.get("P" + (1001 + (i * 7919) % n)) != null ? 1 : 0;
            double hash = (System.nanoTime() - t0) / 1e6;
            System.out.printf("%,8d %6d %18.1f %26.1f %17.1fx%n", n, q, scan, hash, scan / hash);
            c2.printf("%d,%d,%.2f,%.2f%n", n, q, scan, hash);
            Files.delete(text);
        }
        c2.close();
        System.out.println("(checksum " + sink + ")");
    }
}
