/**
 * Task 3 - measures allocate()/release() for the scanning design and the
 * count+stack design. Worst case for the scan: every slot full except the LAST one.
 */
public class AllocBenchmark {
    static void run(String label, int floors, int slotsPerFloor) {
        int[] cfg = new int[floors];
        java.util.Arrays.fill(cfg, slotsPerFloor);
        ParkingFacility scan = new ParkingFacility(cfg);
        FastParkingFacility fast = new FastParkingFacility(cfg);
        int total = floors * slotsPerFloor;
        for (int i = 0; i < total - 1; i++) { scan.allocate("V" + i); fast.allocate("V" + i); }
        int reps = Math.max(2000, 40_000_000 / total);
        long sink = 0;
        for (int w = 0; w < 5; w++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) { int[] p = scan.allocate("X"); sink += p[1]; scan.release(p[0], p[1]); }
            long t1 = System.nanoTime();
            for (int i = 0; i < reps; i++) { int[] p = fast.allocate("X"); sink += p[1]; fast.release(p[0], p[1]); }
            long t2 = System.nanoTime();
            if (w == 4) System.out.printf("%-22s %-8d scan alloc+release = %9.1f ns   count+stack alloc+release = %7.1f ns   (sink=%d)%n",
                    label, total, (t1 - t0) / (double) reps, (t2 - t1) / (double) reps, sink > 0 ? 1 : 0);
        }
    }
    public static void main(String[] args) {
        System.out.println("Worst case: all slots full except the last one");
        run("1 floor  x 20 slots", 1, 20);
        run("3 floors x 50 slots", 3, 50);
        run("5 floors x 100 slots", 5, 100);
        run("20 floors x 500 slots", 20, 500);
        run("50 floors x 2000 slots", 50, 2000);
    }
}
