import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Student B: do thread priorities change the order? 4 CPU-bound stations, 12 repetitions per setting. */
public class PriorityExperiment {
    static volatile long sink;
    static void work() { long s = 0; for (int i = 0; i < 60_000_000; i++) s += i ^ (s >>> 3); sink = s; }

    static String runOnce(int[] prio) throws Exception {
        Queue<String> order = new ConcurrentLinkedQueue<>();
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) {
            final String name = "S" + (i + 1);
            ts[i] = new Thread(() -> { work(); order.add(name); }, name);
            ts[i].setPriority(prio[i]);
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return String.join(" ", order);
    }
    public static void main(String[] args) throws Exception {
        int runs = 12;
        System.out.println("available processors = " + Runtime.getRuntime().availableProcessors());
        int[][] settings = {{5, 5, 5, 5}, {10, 5, 5, 1}};
        String[] label = {"all priority 5 (NORM)", "S1 = 10 (MAX), S2 = S3 = 5, S4 = 1 (MIN)"};
        for (int s = 0; s < 2; s++) {
            System.out.println();
            System.out.println("=== " + label[s] + " ===");
            int[] first = new int[4]; int[] last = new int[4];
            for (int r = 1; r <= runs; r++) {
                String o = runOnce(settings[s]);
                String[] p = o.split(" ");
                first[p[0].charAt(1) - '1']++; last[p[3].charAt(1) - '1']++;
                System.out.printf("run %2d finish order: %s%n", r, o);
            }
            System.out.println("times each station finished FIRST: S1=" + first[0] + " S2=" + first[1] + " S3=" + first[2] + " S4=" + first[3]);
            System.out.println("times each station finished LAST : S1=" + last[0] + " S2=" + last[1] + " S3=" + last[2] + " S4=" + last[3]);
        }
    }
}
