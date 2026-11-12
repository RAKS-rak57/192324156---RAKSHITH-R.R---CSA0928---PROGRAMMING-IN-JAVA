import java.util.concurrent.ThreadLocalRandom;

/** The work of one scanner station: scan `batch` scripts, each taking 1-4 ms. */
public class Scanner1 {
    static int scan(String station, int batch) {
        int scanned = 0;
        for (int i = 0; i < batch; i++) {
            try { Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5)); }       // simulated scanning time
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            scanned++;
        }
        return scanned;
    }
}
