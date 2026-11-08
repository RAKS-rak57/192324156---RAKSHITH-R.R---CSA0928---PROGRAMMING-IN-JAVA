import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Q2 - ArrayList vs LinkedList vs ArrayDeque for a music playlist.
 * Every operation is repeated OPS times on a list of size N,
 * and the whole experiment is repeated REPEATS times and averaged.
 */
public class PlaylistExperiments {

    static class Song {
        int id;
        String title;
        Song(int id) { this.id = id; this.title = "Song" + id; }
    }

    static final int REPEATS = 5;
    static long sink = 0;                       // stops the JIT removing the work

    static List<Song> makeList(String type, int n) {
        List<Song> list = type.equals("ArrayList") ? new ArrayList<Song>() : new LinkedList<Song>();
        for (int i = 0; i < n; i++) list.add(new Song(i));
        return list;
    }

    // ----- one experiment returns time in nanoseconds for OPS operations -----
    static long addEnd(List<Song> list, int ops) {
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) list.add(new Song(i));
        return System.nanoTime() - start;
    }

    static long insertFront(List<Song> list, int ops) {
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) list.add(0, new Song(i));
        return System.nanoTime() - start;
    }

    static long insertMiddle(List<Song> list, int ops) {
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) list.add(list.size() / 2, new Song(i));
        return System.nanoTime() - start;
    }

    static long removeFront(List<Song> list, int ops) {
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) list.remove(0);
        return System.nanoTime() - start;
    }

    static long removeMiddle(List<Song> list, int ops) {
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) list.remove(list.size() / 2);
        return System.nanoTime() - start;
    }

    static long getKth(List<Song> list, int ops, int[] positions) {
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) sink += list.get(positions[i]).id;
        return System.nanoTime() - start;
    }

    static long iterate(List<Song> list) {
        long start = System.nanoTime();
        for (Song s : list) sink += s.id;
        return System.nanoTime() - start;
    }

    // ----- a plain array with manual shifting (alternative concept) -----
    static long arrayInsertFront(int n, int ops) {
        Song[] arr = new Song[n + ops];
        for (int i = 0; i < n; i++) arr[i] = new Song(i);
        int size = n;
        long start = System.nanoTime();
        for (int k = 0; k < ops; k++) {
            for (int i = size; i > 0; i--) arr[i] = arr[i - 1];   // shift everything right
            arr[0] = new Song(k);
            size++;
        }
        return System.nanoTime() - start;
    }

    static long arrayInsertMiddle(int n, int ops) {
        Song[] arr = new Song[n + ops];
        for (int i = 0; i < n; i++) arr[i] = new Song(i);
        int size = n;
        long start = System.nanoTime();
        for (int k = 0; k < ops; k++) {
            for (int i = size; i > size / 2; i--) arr[i] = arr[i - 1];
            arr[size / 2] = new Song(k);
            size++;
        }
        return System.nanoTime() - start;
    }

    // ----- ArrayDeque versions -----
    static long dequeAddEnd(int n, int ops) {
        ArrayDeque<Song> dq = new ArrayDeque<>();
        for (int i = 0; i < n; i++) dq.addLast(new Song(i));
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) dq.addLast(new Song(i));
        return System.nanoTime() - start;
    }

    static long dequeAddFront(int n, int ops) {
        ArrayDeque<Song> dq = new ArrayDeque<>();
        for (int i = 0; i < n; i++) dq.addLast(new Song(i));
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) dq.addFirst(new Song(i));
        return System.nanoTime() - start;
    }

    static long dequeRemoveFront(int n, int ops) {
        ArrayDeque<Song> dq = new ArrayDeque<>();
        for (int i = 0; i < n; i++) dq.addLast(new Song(i));
        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) dq.pollFirst();
        return System.nanoTime() - start;
    }

    static long dequeIterate(int n) {
        ArrayDeque<Song> dq = new ArrayDeque<>();
        for (int i = 0; i < n; i++) dq.addLast(new Song(i));
        long start = System.nanoTime();
        for (Song s : dq) sink += s.id;
        return System.nanoTime() - start;
    }

    public static void main(String[] args) throws Exception {
        int[] sizes = {10000, 50000, 200000};
        final int OPS = 1000;
        String[] ops = {"add at end", "insert at front", "insert in middle", "remove at front",
                        "remove in middle", "get k-th (random)", "iterate whole list"};
        String[] types = {"ArrayList", "LinkedList"};
        Random rnd = new Random(42);

        PrintWriter csv = new PrintWriter("q2_list_results.csv");
        csv.println("size,operation,structure,total_ns_for_ops,ns_per_op,ops_count");
        System.out.println("=== Q2 : time per operation in microseconds (avg of " + REPEATS + " runs, " + OPS + " ops each) ===");

        // warm-up
        for (int w = 0; w < 3; w++) {
            for (String t : types) {
                List<Song> l = makeList(t, 20000);
                addEnd(l, 1000); insertFront(l, 200); removeMiddle(l, 200); iterate(l);
            }
        }

        for (int n : sizes) {
            System.out.println();
            System.out.println("Playlist size N = " + n);
            System.out.printf("%-20s %14s %14s %14s%n", "Operation", "ArrayList", "LinkedList", "ArrayDeque");
            int[] positions = new int[OPS];
            for (int i = 0; i < OPS; i++) positions[i] = rnd.nextInt(n);

            for (int o = 0; o < ops.length; o++) {
                double[] us = new double[3];
                for (int t = 0; t < 3; t++) {
                    long total = 0;
                    int count = (o == 6) ? n : OPS;
                    boolean na = false;
                    for (int r = 0; r < REPEATS; r++) {
                        long ns = 0;
                        if (t < 2) {
                            List<Song> list = makeList(types[t], n);
                            switch (o) {
                                case 0: ns = addEnd(list, OPS); break;
                                case 1: ns = insertFront(list, OPS); break;
                                case 2: ns = insertMiddle(list, OPS); break;
                                case 3: ns = removeFront(list, OPS); break;
                                case 4: ns = removeMiddle(list, OPS); break;
                                case 5: ns = getKth(list, OPS, positions); break;
                                default: ns = iterate(list);
                            }
                        } else {
                            switch (o) {
                                case 0: ns = dequeAddEnd(n, OPS); break;
                                case 1: ns = dequeAddFront(n, OPS); break;
                                case 3: ns = dequeRemoveFront(n, OPS); break;
                                case 6: ns = dequeIterate(n); break;
                                default: na = true;          // no index based operations in ArrayDeque
                            }
                        }
                        total += ns;
                    }
                    if (na) { us[t] = -1; continue; }
                    double avgNs = total / (double) REPEATS;
                    us[t] = avgNs / count / 1000.0;          // microseconds per operation
                    csv.printf("%d,%s,%s,%.0f,%.1f,%d%n", n, ops[o], (t == 0 ? "ArrayList" : t == 1 ? "LinkedList" : "ArrayDeque"), avgNs, avgNs / count, count);
                }
                System.out.printf("%-20s %14s %14s %14s%n", ops[o], fmt(us[0]), fmt(us[1]), fmt(us[2]));
            }
        }
        csv.close();

        // ---------- changed condition: "Up Next" queue ----------
        System.out.println();
        System.out.println("=== Up Next queue: 100,000 operations (addLast, addFirst, removeFirst) on a queue holding N songs ===");
        PrintWriter q = new PrintWriter("q2_upnext_results.csv");
        q.println("size,structure,total_ms_for_100000_mixed_ops");
        System.out.printf("%10s %14s %14s %14s%n", "N", "ArrayList(ms)", "LinkedList(ms)", "ArrayDeque(ms)");
        final int MIXED = 100000;
        for (int n : new int[]{1000, 10000, 50000}) {
            double[] ms = new double[3];
            for (int t = 0; t < 3; t++) {
                long total = 0;
                for (int r = 0; r < REPEATS; r++) {
                    long start;
                    if (t == 0) {
                        List<Song> l = makeList("ArrayList", n);
                        start = System.nanoTime();
                        for (int i = 0; i < MIXED; i++) {
                            l.add(new Song(i)); l.add(0, new Song(i)); l.remove(0);
                        }
                    } else if (t == 1) {
                        LinkedList<Song> l = (LinkedList<Song>) makeList("LinkedList", n);
                        start = System.nanoTime();
                        for (int i = 0; i < MIXED; i++) {
                            l.addLast(new Song(i)); l.addFirst(new Song(i)); l.removeFirst();
                        }
                    } else {
                        ArrayDeque<Song> l = new ArrayDeque<>();
                        for (int i = 0; i < n; i++) l.addLast(new Song(i));
                        start = System.nanoTime();
                        for (int i = 0; i < MIXED; i++) {
                            l.addLast(new Song(i)); l.addFirst(new Song(i)); l.pollFirst();
                        }
                    }
                    total += System.nanoTime() - start;
                }
                ms[t] = total / (double) REPEATS / 1_000_000.0;
                q.printf("%d,%s,%.3f%n", n, (t == 0 ? "ArrayList" : t == 1 ? "LinkedList" : "ArrayDeque"), ms[t]);
            }
            System.out.printf("%10d %14.2f %14.2f %14.2f%n", n, ms[0], ms[1], ms[2]);
        }
        q.close();

        // ---------- plain array with manual shifting ----------
        System.out.println();
        System.out.println("=== Plain array with manual shifting: " + OPS + " inserts (ms) vs ArrayList ===");
        PrintWriter ar = new PrintWriter("q2_array_results.csv");
        ar.println("size,operation,plain_array_ms,ArrayList_ms");
        System.out.printf("%10s %-18s %16s %14s%n", "N", "Operation", "Plain array (ms)", "ArrayList (ms)");
        for (int n : sizes) {
            for (int o = 0; o < 2; o++) {
                long tArr = 0, tList = 0;
                for (int r = 0; r < REPEATS; r++) {
                    if (o == 0) { tArr += arrayInsertFront(n, OPS); tList += insertFront(makeList("ArrayList", n), OPS); }
                    else        { tArr += arrayInsertMiddle(n, OPS); tList += insertMiddle(makeList("ArrayList", n), OPS); }
                }
                double a = tArr / (double) REPEATS / 1e6, b = tList / (double) REPEATS / 1e6;
                String name = (o == 0) ? "insert at front" : "insert in middle";
                System.out.printf("%10d %-18s %16.3f %14.3f%n", n, name, a, b);
                ar.printf("%d,%s,%.3f,%.3f%n", n, name, a, b);
            }
        }
        ar.close();
        System.out.println();
        System.out.println("(checksum " + sink + ")");
    }

    static String fmt(double v) {
        return v < 0 ? "n/a" : String.format("%.3f", v);
    }
}
