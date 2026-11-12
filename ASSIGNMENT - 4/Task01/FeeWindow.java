import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

/** The window: only creates the panel and handles closing. */
public class FeeWindow extends JFrame {
    private final FeePanel panel;

    public FeeWindow(FeeService service) {
        super("Student Fee Payment");
        panel = new FeePanel(service);
        add(panel);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);           // we decide in windowClosing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (panel.isBusy()) {
                    int answer = JOptionPane.showConfirmDialog(FeeWindow.this,
                            "A payment is still in progress. Close anyway?", "Payment in progress", JOptionPane.YES_NO_OPTION);
                    if (answer != JOptionPane.YES_OPTION) return;
                }
                dispose();                                         // no connection is kept open, nothing else to close
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        FeeService service;
        if (Boolean.getBoolean("fee.demo")) {                      // java -Dfee.demo=true FeeWindow   (no database needed)
            service = new FeeService(new InMemoryPaymentRepository(), new SimulatedBank(1500));
        } else {
            String url = System.getProperty("fee.url", "jdbc:mysql://localhost:3306/college");
            String user = System.getProperty("fee.user", "root");
            String pwd = System.getProperty("fee.pwd", "");
            service = new FeeService(new JdbcPaymentRepository(url, user, pwd), new SimulatedBank(1500));
        }
        SwingUtilities.invokeLater(() -> new FeeWindow(service).setVisible(true));
    }
}
