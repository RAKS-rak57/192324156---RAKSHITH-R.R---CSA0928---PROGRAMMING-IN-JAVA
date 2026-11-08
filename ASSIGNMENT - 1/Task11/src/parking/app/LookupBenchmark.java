package parking.app;

import parking.model.*;
import java.time.LocalDateTime;
import java.util.*;

/** Task 11 alternative: find an active ticket on exit - array vs ArrayList vs HashMap. */
public class LookupBenchmark {
    static long usedMemory() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    public static void main(String[] args) {
        System.out.println("Find a ticket by registration number (worst case: the LAST ticket inserted)");
        System.out.printf("%-9s %-18s %-18s %-18s | %-12s %-12s %-12s%n", "n", "array (ns)", "ArrayList (ns)", "HashMap (ns)", "array B/tkt", "list B/tkt", "map B/tkt");
        User u = new Visitor("v");
        LocalDateTime t = LocalDateTime.of(2026, 11, 7, 8, 0);
        int[] sizes = {100, 1_000, 10_000, 100_000};
        for (int n : sizes) {
            Vehicle[] veh = new Vehicle[n];
            String[] regs = new String[n];
            for (int i = 0; i < n; i++) { regs[i] = String.format("TN%06d", i); veh[i] = new Car(regs[i]); }
            long m0 = usedMemory();
            ParkingTicket[] arr = new ParkingTicket[n];
            for (int i = 0; i < n; i++) arr[i] = new ParkingTicket(veh[i], u, 0, i, t);
            long m1 = usedMemory();
            ArrayList<ParkingTicket> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(arr[i]);
            long m2 = usedMemory();
            HashMap<String, ParkingTicket> map = new HashMap<>();
            for (int i = 0; i < n; i++) map.put(regs[i], arr[i]);
            long m3 = usedMemory();

            String target = regs[n - 1];
            int reps = Math.max(200, 4_000_000 / n);
            long sink = 0;
            double ta = Double.MAX_VALUE, tl = Double.MAX_VALUE, tm = Double.MAX_VALUE;
            for (int w = 0; w < 10; w++) {
                long a0 = System.nanoTime();
                for (int r = 0; r < reps; r++) { for (int i = 0; i < n; i++) if (arr[i].getVehicle().getRegNo().equals(target)) { sink += i; break; } }
                long a1 = System.nanoTime();
                for (int r = 0; r < reps; r++) { for (int i = 0; i < list.size(); i++) if (list.get(i).getVehicle().getRegNo().equals(target)) { sink += i; break; } }
                long a2 = System.nanoTime();
                for (int r = 0; r < reps; r++) sink += map.get(target).getSlot();
                long a3 = System.nanoTime();
                if (w >= 3) { ta = Math.min(ta, (a1 - a0) / (double) reps); tl = Math.min(tl, (a2 - a1) / (double) reps); tm = Math.min(tm, (a3 - a2) / (double) reps); }  // best of 7 warmed-up rounds
            }
            String ml = n >= 10_000 ? String.valueOf((m2 - m1) / n) : "n/a";   // heap measurement is too noisy for small n
            String mm = n >= 10_000 ? String.valueOf((m3 - m2) / n) : "n/a";
            System.out.printf("%-9d %-18.1f %-18.1f %-18.1f | %-12d %-12s %-12s (sink=%d)%n", n, ta, tl, tm, 4, ml, mm, sink > 0 ? 1 : 0);
        }
        System.out.println("B/tkt = extra bytes per ticket for the CONTAINER only (array slot = one 4-byte reference with compressed oops;");
        System.out.println("ArrayList adds growth slack; HashMap adds a Node (32 B) + table slot per entry). Ticket objects themselves are shared by all three.");
    }
}
