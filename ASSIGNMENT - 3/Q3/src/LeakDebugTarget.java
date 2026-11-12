import java.io.File;
/** Small program used for the step-through debugging session (see output/jdb_session.txt). */
public class LeakDebugTarget {
    public static void main(String[] args) throws Exception {
        try {
            ReportGenerator.leaky(ReportGenerator.listStores("input/stores_bad"), new File("/tmp/debug_report.csv"));
        } catch (ReportGenerator.ReportException e) {
            System.out.println("report failed at " + e.file);
        }
        System.out.println("open handles at exit: " + Tracked.openCount());
    }
}
