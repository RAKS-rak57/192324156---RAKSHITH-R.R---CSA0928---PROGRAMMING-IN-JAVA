import java.util.*;

/**
 * Task 1 - Alternative concept: vehicles as OBJECTS versus vehicles as
 * PLAIN STRINGS in PARALLEL ARRAYS (regNo[], type[], entryTime[]).
 * Compares (a) find-by-registration-number and (b) adding a new attribute.
 */
public class Task01Demo {

    // ---------- Design A : one object per vehicle ----------
    static class VehicleRecord {
        final String regNo;
        final String type;
        final long entryTime;
        String owner;                       // new attribute = ONE new field
        VehicleRecord(String regNo, String type, long entryTime) {
            this.regNo = regNo; this.type = type; this.entryTime = entryTime;
        }
    }

    static int findObjects(VehicleRecord[] v, String reg) {
        for (int i = 0; i < v.length; i++) if (v[i].regNo.equals(reg)) return i;
        return -1;
    }

    // ---------- Design B : parallel arrays ----------
    static int findParallel(String[] regNo, String reg) {
        for (int i = 0; i < regNo.length; i++) if (regNo[i].equals(reg)) return i;
        return -1;
    }

    static long usedMemory() {
        System.gc(); System.gc();
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    public static void main(String[] args) {
        System.out.println("=== Task 1 : objects vs parallel arrays ===");
        int[] sizes = {1_000, 10_000, 100_000};
        System.out.printf("%-10s %-22s %-22s%n", "n", "objects (ns/search)", "parallel (ns/search)");
        for (int n : sizes) {
            VehicleRecord[] objs = new VehicleRecord[n];
            String[] regNo = new String[n];
            String[] type = new String[n];
            long[] entry = new long[n];
            for (int i = 0; i < n; i++) {
                String r = "TN" + String.format("%06d", i);
                objs[i] = new VehicleRecord(r, "CAR", i);
                regNo[i] = r; type[i] = "CAR"; entry[i] = i;
            }
            String target = "TN" + String.format("%06d", n - 1);   // worst case: last element
            int reps = Math.max(200, 20_000_000 / n);
            int sink = 0;
            for (int w = 0; w < reps; w++) { sink += findObjects(objs, target) + findParallel(regNo, target); } // warm-up
            long t0 = System.nanoTime();
            for (int w = 0; w < reps; w++) sink += findObjects(objs, target);
            long t1 = System.nanoTime();
            for (int w = 0; w < reps; w++) sink += findParallel(regNo, target);
            long t2 = System.nanoTime();
            System.out.printf("%-10d %-22d %-22d (sink=%d)%n", n, (t1 - t0) / reps, (t2 - t1) / reps, sink > 0 ? 1 : 0);
        }

        // Memory per vehicle (measured, n = 200,000). Strings are shared by both designs, so only the container is compared.
        int n = 200_000;
        String[] shared = new String[n];
        for (int i = 0; i < n; i++) shared[i] = "TN" + i;
        String carType = "CAR";
        long base = usedMemory();
        VehicleRecord[] objs = new VehicleRecord[n];
        for (int i = 0; i < n; i++) objs[i] = new VehicleRecord(shared[i], carType, i);
        long afterObjs = usedMemory();
        String[] rA = new String[n]; String[] tA = new String[n]; long[] eA = new long[n];
        for (int i = 0; i < n; i++) { rA[i] = shared[i]; tA[i] = carType; eA[i] = i; }
        long afterPar = usedMemory();
        System.out.println();
        System.out.printf("Measured container memory per vehicle: objects ~ %d bytes, parallel arrays ~ %d bytes%n",
                (afterObjs - base) / n, (afterPar - afterObjs) / n);
        System.out.println("(objects: 12-16 byte header + 2 refs + long + owner ref, padded;  parallel: 2 refs + 1 long per index)");
        System.out.println("keep-alive: " + objs.length + " " + rA.length + " " + tA.length + " " + eA.length);

        // Adding a new attribute 'owner'
        System.out.println();
        System.out.println("Adding attribute 'owner':");
        System.out.println(" objects  -> add one field to VehicleRecord (1 place changed, existing code untouched)");
        System.out.println(" parallel -> add a 4th array AND update every add/remove/copy/shift routine (k+1 arrays kept in sync)");
        objs[5].owner = "Priya";
        System.out.println(" objects  demo: objs[5].owner = " + objs[5].owner);
    }
}
