import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * ALTERNATIVE to the text file (Task A1): fixed-length records in a RandomAccessFile.
 * Record i starts at byte i * RECORD_SIZE, so a pass is found with ONE seek: O(1).  Pass ids must be dense: P1001 -> index 0, P1002 -> index 1 ...
 * Record layout (92 bytes): passId 8 | regNo 12 | name 30 | route int 4 | stop 20 | validity 10 | amount double 8
 */
public class FixedRecordStore implements AutoCloseable {
    public static final int RECORD_SIZE = 8 + 12 + 30 + 4 + 20 + 10 + 8;
    public static final int FIRST_ID = 1001;
    private final RandomAccessFile raf;

    public FixedRecordStore(Path file) throws IOException { raf = new RandomAccessFile(file.toFile(), "rw"); }

    static int indexOf(String passId) { return Integer.parseInt(passId.substring(1)) - FIRST_ID; }

    private static void putText(byte[] rec, int off, int width, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        int n = Math.min(b.length, width);
        System.arraycopy(b, 0, rec, off, n);
        for (int i = n; i < width; i++) rec[off + i] = ' ';                // padding = the wasted space
    }
    private static String getText(byte[] rec, int off, int width) { return new String(rec, off, width, StandardCharsets.UTF_8).stripTrailing(); }

    public void put(BusPass p) throws IOException {
        byte[] rec = new byte[RECORD_SIZE];
        putText(rec, 0, 8, p.passId); putText(rec, 8, 12, p.regNo); putText(rec, 20, 30, p.name);
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(rec);
        bb.position(50); bb.putInt(p.route);
        putText(rec, 54, 20, p.stop); putText(rec, 74, 10, p.validity.toString());
        bb.position(84); bb.putDouble(p.amount);
        raf.seek((long) indexOf(p.passId) * RECORD_SIZE);
        raf.write(rec);
    }

    /** O(1): one seek and one read of RECORD_SIZE bytes. */
    public BusPass find(String passId) throws IOException {
        long pos = (long) indexOf(passId) * RECORD_SIZE;
        if (pos < 0 || pos + RECORD_SIZE > raf.length()) return null;
        byte[] rec = new byte[RECORD_SIZE];
        raf.seek(pos);
        raf.readFully(rec);
        if (!getText(rec, 0, 8).equals(passId)) return null;               // empty slot or other record
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(rec);
        return new BusPass(getText(rec, 0, 8), getText(rec, 8, 12), getText(rec, 20, 30), bb.getInt(50), getText(rec, 54, 20),
                LocalDate.parse(getText(rec, 74, 10)), bb.getDouble(84));
    }

    @Override public void close() throws IOException { raf.close(); }
}
