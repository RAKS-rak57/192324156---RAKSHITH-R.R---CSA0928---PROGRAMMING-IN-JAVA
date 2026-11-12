package portal_unchecked;
import java.io.IOException;
/** STORAGE layer: the only layer that knows about disks. Total disk quota 500 KB. */
public class Storage {
    private long usedKb = 0;
    private final long quotaKb = 500;
    public void save(String fileName, long sizeKb) {
        try {
            if (usedKb + sizeKb > quotaKb) throw new IOException("Disk quota exceeded");   // the low-level failure
            usedKb += sizeKb;
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName, e);        // wrap, keep the cause
        }
    }
    /** Same failure, but the cause is thrown away (used to show what chaining protects). */
    public void saveLossy(String fileName, long sizeKb) {
        try {
            if (usedKb + sizeKb > quotaKb) throw new IOException("Disk quota exceeded");
            usedKb += sizeKb;
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName);           // cause LOST
        }
    }
}
