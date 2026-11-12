import java.awt.event.ActionEvent;
import javax.swing.*;

/** Headless demonstration of Task 2: drives the real AppointmentPanel / AppointmentWindow. Not part of the application. */
public class AppointmentHarness {
    static void waitIdle(AppointmentPanel p) throws Exception {
        long t0 = System.currentTimeMillis();
        while (p.isBusy() && System.currentTimeMillis() - t0 < 5000) Thread.sleep(10);
        HeadlessRender.edtIdle();
    }
    static void shot(AppointmentPanel p, String name) throws Exception {
        HeadlessRender.save(HeadlessRender.render(p, 430, 250, "Outpatient Appointment Desk"), "../screenshots/task02/" + name);
    }
    static String doctors(AppointmentPanel p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.doctorBox().getItemCount(); i++) sb.append(i == 0 ? "" : ", ").append(p.doctorBox().getItemAt(i));
        return "[" + sb + "]";
    }
    public static void main(String[] args) throws Exception {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepository());
        final AppointmentWindow[] w = new AppointmentWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new AppointmentWindow(service));      // JFrame (headless shim)
        AppointmentPanel p = w[0].panel();
        p.setSize(430, 250);
        shot(p, "t2_01_initial.png");
        System.out.println("[1] window created. Doctor list: " + doctors(p));

        SwingUtilities.invokeAndWait(() -> p.departmentBox().setSelectedItem("Cardiology"));
        System.out.println("[2] department = Cardiology  -> doctor list: " + doctors(p));
        shot(p, "t2_02_cardiology.png");
        SwingUtilities.invokeAndWait(() -> p.departmentBox().setSelectedItem("Paediatrics"));
        System.out.println("[3] department = Paediatrics -> doctor list: " + doctors(p));
        shot(p, "t2_03_paediatrics.png");

        SwingUtilities.invokeAndWait(() -> p.bookButton().doClick());
        waitIdle(p);
        System.out.println("[4] Book with empty patient ID -> " + p.statusText());
        shot(p, "t2_04_error.png");

        SwingUtilities.invokeAndWait(() -> { p.patientIdField().setText("P1001"); p.doctorBox().setSelectedIndex(1); p.slotBox().setSelectedItem("10:00"); });
        System.out.println("[5] in-progress booking? " + p.isBookingInProgress() + "  -> closing asks for confirmation");
        JOptionPane.nextConfirm = JOptionPane.NO_OPTION;
        System.out.println("    receptionist answers NO  -> window closes? " + w[0].confirmClose());
        JOptionPane.nextConfirm = JOptionPane.YES_OPTION;
        System.out.println("    receptionist answers YES -> window closes? " + w[0].confirmClose());
        for (String line : JOptionPane.log) System.out.println("    " + line);
        JOptionPane.log.clear();

        SwingUtilities.invokeAndWait(() -> p.bookButton().doClick());
        waitIdle(p);
        System.out.println("[6] Book -> " + p.statusText());
        shot(p, "t2_05_success.png");
        System.out.println("[7] form cleared after success; in-progress now? " + p.isBookingInProgress() + "  -> closing needs no question: " + w[0].confirmClose());

        SwingUtilities.invokeAndWait(() -> { p.patientIdField().setText("P1002"); p.departmentBox().setSelectedItem("Paediatrics"); p.doctorBox().setSelectedIndex(1); p.slotBox().setSelectedItem("10:00"); });
        SwingUtilities.invokeAndWait(() -> p.bookButton().doClick());
        waitIdle(p);
        System.out.println("[8] Same doctor and slot, other patient -> " + p.statusText());
        shot(p, "t2_06_slot_taken.png");

        System.out.println();
        System.out.println("Events handled by the panel, in order:");
        int i = 1;
        for (String line : p.trace()) System.out.println("  " + (i++) + ". " + line);
    }
}
