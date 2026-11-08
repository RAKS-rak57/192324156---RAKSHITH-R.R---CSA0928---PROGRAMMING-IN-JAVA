package parking.app;

import parking.model.*;
import parking.service.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Command-line driver. Reads commands from standard input (see input/commands.txt):
 *   FACILITY 2 3 5                          floors with 2, 3 and 5 slots
 *   ENTER   TYPE REG ROLE PASS TIMESTAMP     e.g. ENTER CAR TN02B2 STUDENT PASS 2026-11-07T08:00
 *   EXIT    REG TIMESTAMP KWH
 *   SUMMARY
 */
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        ParkingService service = null;
        int lineNo = 0;
        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            lineNo++;
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            try {
                switch (p[0].toUpperCase()) {
                    case "FACILITY": {
                        int[] slots = new int[p.length - 1];
                        for (int i = 1; i < p.length; i++) slots[i - 1] = Integer.parseInt(p[i]);
                        service = new ParkingService(new ParkingFacility(slots));
                        System.out.println("Facility created: " + slots.length + " floors, " + service.getFacility().totalSlots() + " slots");
                        break;
                    }
                    case "ENTER": {
                        Vehicle v = VehicleFactory.create(p[1], p[2]);
                        LocalDate expiry = p[4].equalsIgnoreCase("PASS") ? LocalDate.of(2026, 12, 31) : null;
                        User u;
                        switch (p[3].toUpperCase()) {
                            case "STUDENT": u = new Student("student-" + p[2], expiry); break;
                            case "STAFF":   u = new Staff("staff-" + p[2], expiry); break;
                            case "VISITOR": u = new Visitor("visitor-" + p[2]); break;
                            default: throw new IllegalArgumentException("Unknown role: " + p[3]);
                        }
                        ParkingTicket t = service.enter(v, u, LocalDateTime.parse(p[5]));
                        System.out.println("ENTER " + v + " (" + u.role() + (expiry != null ? ", monthly pass" : "") + ") -> " + t);
                        break;
                    }
                    case "EXIT": {
                        Receipt r = service.exit(p[1], LocalDateTime.parse(p[2]), Integer.parseInt(p[3]));
                        System.out.printf("EXIT  %s after %d min: parking Rs %.1f%s, charging Rs %.1f -> PAY Rs %.1f%n",
                                r.regNo(), r.minutes(), r.parkingFee(), r.exempt() ? " (waived: pass)" : "", r.chargingFee(), r.totalPayable());
                        break;
                    }
                    case "SUMMARY":
                        System.out.printf("SUMMARY: vehicles inside = %d, free slots = %d, collection today = Rs %.1f%n",
                                service.activeCount(), service.getFacility().freeSlots(), service.getCollectedToday());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown command: " + p[0]);
                }
            } catch (RuntimeException e) {
                System.out.println("line " + lineNo + " ERROR (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }
    }
}
