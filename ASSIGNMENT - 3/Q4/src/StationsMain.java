import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Four scanner stations. args[0] = "thread" or "runnable". Prints the order in which the stations finished. */
public class StationsMain {
    static final Queue<String> FINISHED = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws InterruptedException {
        String mode = args.length > 0 ? args[0] : "thread";
        int batch = 20;
        int total = 0;
        if (mode.equals("thread")) {
            StationThread[] st = new StationThread[4];
            for (int i = 0; i < 4; i++) { st[i] = new StationThread("S" + (i + 1), batch); st[i].start(); }
            System.out.println("alive right after start(): " + st[0].isAlive() + " " + st[1].isAlive() + " " + st[2].isAlive() + " " + st[3].isAlive());
            for (StationThread t : st) t.join();                       // wait for every station
            System.out.println("alive after join()       : " + st[0].isAlive() + " " + st[1].isAlive() + " " + st[2].isAlive() + " " + st[3].isAlive());
            for (StationThread t : st) total += t.scanned;
        } else {
            StationRunnable[] tasks = new StationRunnable[4]; Thread[] th = new Thread[4];
            for (int i = 0; i < 4; i++) { tasks[i] = new StationRunnable("S" + (i + 1), batch); th[i] = new Thread(tasks[i], "T" + (i + 1)); th[i].start(); }
            for (Thread t : th) t.join();
            for (StationRunnable t : tasks) total += t.scanned;
        }
        System.out.println("finish order: " + String.join(" ", FINISHED) + "   total scripts scanned = " + total);
    }
}
