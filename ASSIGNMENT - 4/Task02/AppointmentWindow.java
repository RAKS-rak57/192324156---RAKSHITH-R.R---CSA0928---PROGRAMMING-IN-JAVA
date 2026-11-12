import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

public class AppointmentWindow extends JFrame {
    private final AppointmentPanel panel;

    public AppointmentWindow(AppointmentService service) {
        super("Outpatient Appointment Desk");
        panel = new AppointmentPanel(service);
        add(panel);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (confirmClose()) dispose(); }
        });
        pack();
        setLocationRelativeTo(null);
    }

    /** Asks the receptionist only when a booking is in progress. */
    boolean confirmClose() {
        if (!panel.isBookingInProgress()) return true;
        int answer = JOptionPane.showConfirmDialog(this, "A booking is in progress. Close the window anyway?",
                "Confirm close", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }

    public AppointmentPanel panel() { return panel; }

    public static void main(String[] args) {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepository());
        SwingUtilities.invokeLater(() -> new AppointmentWindow(service).setVisible(true));
    }
}
