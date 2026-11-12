import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Instrumented readers/writers: every open stream registers itself and removes itself in close(). */
public class Tracked {
    static final Set<Closeable> OPEN = Collections.synchronizedSet(new LinkedHashSet<>());
    static int peak = 0;

    static synchronized void opened(Closeable c) { OPEN.add(c); peak = Math.max(peak, OPEN.size()); }
    static void reset() { OPEN.clear(); peak = 0; }
    static int openCount() { return OPEN.size(); }
    static void closeAllLeaked() {
        for (Closeable c : new ArrayList<>(OPEN)) { try { c.close(); } catch (IOException ignored) { } }
    }

    /** Number of file descriptors of THIS process that point to a path containing one of the markers (Linux only; -1 elsewhere). */
    static int osOpenFds(String... markers) {
        Path dir = Paths.get("/proc/self/fd");
        if (!Files.isDirectory(dir)) return -1;
        int n = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                try {
                    String target = Files.readSymbolicLink(p).toString();
                    for (String m : markers) if (target.contains(m)) { n++; break; }
                } catch (IOException ignored) { }
            }
        } catch (IOException e) { return -1; }
        return n;
    }

    static class Reader extends BufferedReader {
        Reader(File f) throws FileNotFoundException { super(new FileReader(f)); opened(this); }
        @Override public void close() throws IOException { OPEN.remove(this); super.close(); }
    }
    static class Writer extends BufferedWriter {
        Writer(File f) throws IOException { super(new FileWriter(f)); opened(this); }
        @Override public void close() throws IOException { OPEN.remove(this); super.close(); }
    }
}
