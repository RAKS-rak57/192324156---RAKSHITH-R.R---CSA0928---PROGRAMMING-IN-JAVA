import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The text-file store designed in Task A1.  Operations: add a pass, find a pass by ID, list the passes of a route.
 * Streams: BufferedWriter (append, one write per record) and BufferedReader (line by line).  Damaged lines are skipped and reported.
 */
public class BusPassStore {
    private final Path file;
    /** One entry per damaged line met during the LAST scan: "line 7: expected 8 fields but found 2". */
    public final List<String> damaged = new ArrayList<>();

    public BusPassStore(Path file) { this.file = file; }

    /** Adds a pass at the end of the file (rejects a duplicate pass id). */
    public void add(BusPass p) throws IOException {
        if (find(p.passId) != null) throw new IOException("pass id already exists: " + p.passId);
        append(p);
    }

    /** Append without the duplicate scan (used for bulk loading, O(1) per record). */
    public void append(BusPass p) throws IOException {
        repairMissingNewline();
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(p.toLine() + "\n");                         // the whole record in ONE write: a power cut can only cut the last line
        }
    }

    /** If the last write was cut off (no line break at the end), start a new line so that the next record is not glued to the broken one. */
    private void repairMissingNewline() throws IOException {
        if (!Files.exists(file) || Files.size(file) == 0) return;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.seek(raf.length() - 1);
            if (raf.read() != '\n') { raf.seek(raf.length()); raf.write('\n'); }
        }
    }

    /** Linear scan, O(n). Returns null when the id is not found OR the file does not exist. */
    public BusPass find(String passId) throws IOException {
        BusPass[] found = new BusPass[1];
        scan(p -> { if (p.passId.equals(passId)) { found[0] = p; return false; } return true; });
        return found[0];
    }

    public List<BusPass> listByRoute(int route) throws IOException {
        List<BusPass> out = new ArrayList<>();
        scan(p -> { if (p.route == route) out.add(p); return true; });
        return out;
    }

    public interface Visitor { boolean visit(BusPass p); }       // return false to stop the scan

    public void scan(Visitor v) throws IOException {
        damaged.clear();
        if (!Files.exists(file)) return;                           // missing file = empty store (not a crash)
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line; int n = 0;
            while ((line = r.readLine()) != null) {
                n++;
                try { if (!v.visit(BusPass.fromLine(line))) return; }
                catch (DamagedRecordException e) { damaged.add("line " + n + ": " + e.getMessage()); }   // skip and remember
            }
        }
    }
}
