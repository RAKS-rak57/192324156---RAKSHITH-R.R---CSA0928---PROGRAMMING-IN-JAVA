import java.util.List;

public class AppointmentServiceTest {
    static int pass = 0, fail = 0;
    static void check(String name, BookingResult r, boolean expect, String part) {
        boolean ok = r.success == expect && r.message.contains(part);
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-34s %s%n", ok ? "PASS" : "FAIL", name, r.message);
    }
    public static void main(String[] args) {
        AppointmentService s = new AppointmentService(new InMemoryAppointmentRepository());
        System.out.println("Departments : " + s.departments());
        for (String d : s.departments()) System.out.println("Doctors of " + d + " : " + s.doctorsFor(d));
        System.out.println();
        List<Doctor> card = s.doctorsFor("Cardiology");
        Doctor anand = card.get(0), meera = card.get(1), paed = s.doctorsFor("Paediatrics").get(0);
        check("valid booking",                s.book("P1001", "Cardiology", anand, "10:00"), true, "confirmed");
        check("same slot, same doctor",       s.book("P1002", "Cardiology", anand, "10:00"), false, "already booked");
        check("same slot, other doctor",      s.book("P1002", "Cardiology", meera, "10:00"), true, "confirmed");
        check("empty patient id",             s.book("", "Cardiology", anand, "11:00"), false, "Enter the patient ID");
        check("bad patient id",               s.book("1001", "Cardiology", anand, "11:00"), false, "must look like");
        check("no department",                s.book("P1003", AppointmentService.PROMPT, anand, "11:00"), false, "Select a department");
        check("no doctor",                    s.book("P1003", "Cardiology", null, "11:00"), false, "Select a doctor");
        check("doctor of another department", s.book("P1003", "Cardiology", paed, "11:00"), false, "does not belong");
        check("no slot",                      s.book("P1003", "Cardiology", anand, AppointmentService.PROMPT), false, "Select a time slot");
        System.out.println("\nResult: " + pass + " passed, " + fail + " failed");
    }
}
