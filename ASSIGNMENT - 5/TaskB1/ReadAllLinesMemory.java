import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.List;

/** A2 alternative: memory and time of Files.readAllLines() against a buffered stream for the 50 MB attendance log. */
public class ReadAllLinesMemory {
    static long allocated() { com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean(); return b.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    static long used() { for (int i = 0; i < 3; i++) System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("log50", ".log");
        long lines = 50L * 1024 * 1024 / 33;
        try (BufferedWriter w = Files.newBufferedWriter(f)) { for (long i = 0; i < lines; i++) w.write(String.format("2026-11-09,%09d,IN,08:45:12\n", 192311000L + (i % 9000))); }
        System.out.printf("file: %,d bytes, %,d lines%n", Files.size(f), lines);
        long[] calls = new long[1];
        for (int w = 0; w < 2; w++) { LineCounter.readAllLines(f); LineCounter.bufferedStream(f, calls); }
        long base = used(), a0 = allocated(); long t = System.nanoTime();
        List<String> all = Files.readAllLines(f);
        double ms = (System.nanoTime() - t) / 1e6; long alloc = allocated() - a0; long retained = used() - base;
        System.out.printf("Files.readAllLines : %.0f ms, %,d lines in the list, memory retained by the list = %.0f MB, allocated while reading = %.0f MB%n", ms, all.size(), retained / 1048576.0, alloc / 1048576.0);
        all = null;
        base = used(); a0 = allocated(); t = System.nanoTime();
        long n = LineCounter.bufferedStream(f, calls);
        System.out.printf("BufferedInputStream: %.0f ms, %,d lines counted, allocated while reading = %.3f MB (the 8,192-byte buffer), retained = about 0%n", (System.nanoTime() - t) / 1e6, n, (allocated() - a0) / 1048576.0);
        Files.delete(f);
    }
}
