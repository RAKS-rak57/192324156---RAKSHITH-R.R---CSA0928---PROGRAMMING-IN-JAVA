import java.io.*;
import java.nio.file.*;

/** Tasks A2 / B1.3: how many read calls and how much time does each line counter need for 1, 10 and 50 MB? */
public class BufferingBenchmark {
    static final int LINE = 33;                                              // bytes per generated log line
    static Path makeLog(long megabytes) throws IOException {
        Path f = Files.createTempFile("attendance" + megabytes + "MB", ".log");
        long lines = megabytes * 1024 * 1024 / LINE;
        try (BufferedWriter w = Files.newBufferedWriter(f)) {
            for (long i = 0; i < lines; i++) w.write(String.format("2026-11-09,%09d,IN,08:45:12\n", 192311000L + (i % 9000)));
        }
        return f;
    }

    interface Counter { long run(Path f, long[] calls) throws IOException; }

    public static void main(String[] args) throws Exception {
        long[] sizes = {1, 10, 50};
        String[] names = {"unbuffered read()", "BufferedInputStream", "byte[64 KB] buffer", "BufferedReader", "Files.readAllLines"};
        Counter[] cs = {
            LineCounter::unbuffered, LineCounter::bufferedStream, (f, c) -> LineCounter.byteArray(f, 65536, c),
            (f, c) -> { c[0] = -1; return LineCounter.bufferedReader(f); }, (f, c) -> { c[0] = -1; return LineCounter.readAllLines(f); }};
        PrintWriter csv = new PrintWriter("buffering_results.csv");
        csv.println("size_MB,file_bytes,method,time_ms,read_calls,lines");
        System.out.println("=== B1.3 Count the lines of an attendance log (time = best of the runs, in milliseconds) ===");
        System.out.printf("%7s %12s %-22s %12s %16s %10s%n", "size", "bytes", "method", "time (ms)", "read calls", "lines");
        for (int w = 0; w < 2; w++) { Path t = makeLog(1); for (Counter c : cs) c.run(t, new long[1]); Files.delete(t); }     // warm-up
        for (long mb : sizes) {
            Path f = makeLog(mb);
            long bytes = Files.size(f);
            for (int m = 0; m < cs.length; m++) {
                int runs = (m == 0) ? (mb >= 50 ? 1 : 3) : 5;
                double best = Double.MAX_VALUE; long lines = 0; long[] calls = new long[1];
                for (int r = 0; r < runs; r++) {
                    long t0 = System.nanoTime();
                    lines = cs[m].run(f, calls);
                    best = Math.min(best, (System.nanoTime() - t0) / 1e6);
                }
                System.out.printf("%5d MB %,12d %-22s %12.1f %16s %,10d%n", mb, bytes, names[m], best, calls[0] < 0 ? "(not counted)" : String.format("%,d", calls[0]), lines);
                csv.printf("%d,%d,%s,%.2f,%d,%d%n", mb, bytes, names[m], best, calls[0], lines);
            }
            Files.delete(f);
        }
        csv.close();

        // ---------------- A2.3: when does a buffer not help? ----------------
        System.out.println();
        System.out.println("=== A2.3 When buffering gives little or no benefit (10 MB file, best of 5) ===");
        Path f = makeLog(10);
        double direct = Double.MAX_VALUE, wrapped = Double.MAX_VALUE;
        long[] c1 = new long[1], c2 = new long[1];
        byte[] big = new byte[65536];
        for (int r = 0; r < 5; r++) {
            long t0 = System.nanoTime();
            try (LineCounter.CountingInputStream in = new LineCounter.CountingInputStream(new FileInputStream(f.toFile()))) { while (in.read(big, 0, big.length) != -1) { } c1[0] = in.calls; }
            direct = Math.min(direct, (System.nanoTime() - t0) / 1e6);
            t0 = System.nanoTime();
            try (LineCounter.CountingInputStream raw = new LineCounter.CountingInputStream(new FileInputStream(f.toFile())); BufferedInputStream in = new BufferedInputStream(raw)) { while (in.read(big, 0, big.length) != -1) { } c2[0] = raw.calls; }
            wrapped = Math.min(wrapped, (System.nanoTime() - t0) / 1e6);
        }
        System.out.printf("(a) reads of 64 KB blocks: FileInputStream %.1f ms (%,d calls)  |  wrapped in BufferedInputStream %.1f ms (%,d calls)%n", direct, c1[0], wrapped, c2[0]);
        byte[] mem = new byte[10 * 1024 * 1024];
        double plain = Double.MAX_VALUE, buf = Double.MAX_VALUE; long sink = 0;
        for (int r = 0; r < 5; r++) {
            long t0 = System.nanoTime();
            try (InputStream in = new ByteArrayInputStream(mem)) { int b; while ((b = in.read()) != -1) sink += b; }
            plain = Math.min(plain, (System.nanoTime() - t0) / 1e6);
            t0 = System.nanoTime();
            try (InputStream in = new BufferedInputStream(new ByteArrayInputStream(mem))) { int b; while ((b = in.read()) != -1) sink += b; }
            buf = Math.min(buf, (System.nanoTime() - t0) / 1e6);
        }
        System.out.printf("(b) data already in memory (ByteArrayInputStream, byte by byte): plain %.1f ms  |  buffered %.1f ms%n", plain, buf);
        Files.delete(f);
        System.out.println("(checksum " + sink + ")");
    }
}
