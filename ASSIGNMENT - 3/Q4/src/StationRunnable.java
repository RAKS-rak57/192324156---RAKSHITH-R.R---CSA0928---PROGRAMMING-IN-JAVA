/** Way 2: IMPLEMENT Runnable (the task is separate from the thread that runs it). */
public class StationRunnable implements Runnable {
    private final String name; private final int batch;
    volatile int scanned;
    StationRunnable(String name, int batch) { this.name = name; this.batch = batch; }
    @Override public void run() {
        scanned = Scanner1.scan(name, batch);
        StationsMain.FINISHED.add(name);
    }
}
