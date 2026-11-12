/** Way 1: EXTEND Thread. */
public class StationThread extends Thread {
    private final int batch;
    volatile int scanned;
    StationThread(String name, int batch) { super(name); this.batch = batch; }
    @Override public void run() {
        scanned = Scanner1.scan(getName(), batch);
        StationsMain.FINISHED.add(getName());
    }
}
