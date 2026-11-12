import campus.reg.dao.InMemoryRegistrationDao;
import campus.reg.model.CampusEvent;
import campus.reg.service.RegistrationService;
import campus.reg.ui.*;
import java.awt.Component;
import javax.swing.*;

/** Headless demonstration of the whole Swing application (real MainFrame, RegisterPanel, RegistrationsPanel, SwingWorkers). */
public class RegistrationHarness {
    static MainFrame frame;
    static RegisterPanel reg;
    static RegistrationsPanel list;
    static final String DIR = "../screenshots/task06/";

    static void waitIdle() throws Exception {
        long t0 = System.currentTimeMillis();
        while ((reg.isBusy() || list.isLoading()) && System.currentTimeMillis() - t0 < 5000) Thread.sleep(10);
        HeadlessRender.edtIdle();
    }
    static void shot(String name) throws Exception { HeadlessRender.save(FrameRender.of(frame), DIR + name); }
    static void click(JButton b) throws Exception { SwingUtilities.invokeAndWait(b::doClick); waitIdle(); }
    static void fill(String r, String n, String m, int eventIndex) throws Exception {
        SwingUtilities.invokeAndWait(() -> { reg.regNoField().setText(r); reg.nameField().setText(n); reg.emailField().setText(m); reg.eventBox().setSelectedIndex(eventIndex); });
    }
    static void printTable() {
        for (int i = 0; i < list.table().getRowCount(); i++) {
            StringBuilder sb = new StringBuilder("      ");
            for (int c = 0; c < list.table().getColumnCount(); c++) sb.append(String.format("%-22s", list.table().getValueAt(i, c)));
            System.out.println(sb);
        }
    }

    public static void main(String[] args) throws Exception {
        RegistrationService service = new RegistrationService(InMemoryRegistrationDao.withDemoData());
        SwingUtilities.invokeAndWait(() -> { frame = new MainFrame(service, 5); frame.setVisible(true); });
        reg = frame.registerPanel(); list = frame.listPanel();

        System.out.println("=== Menu bar ===");
        JMenuBar bar = frame.getJMenuBar();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu m = bar.getMenu(i);
            StringBuilder sb = new StringBuilder(m.getText() + " : ");
            for (int k = 0; k < m.getItemCount(); k++) sb.append(m.getItem(k) == null ? "| " : m.getItem(k).getText() + "  ");
            System.out.println("  " + sb);
        }
        System.out.println();
        shot("t6_01_register_screen.png");

        System.out.println("=== Stage 5 demonstration through the real window ===");
        click(reg.registerButton());
        System.out.println("[invalid]   empty form, click Register  -> " + reg.statusText());
        shot("t6_02_invalid_empty.png");

        fill("192319010", "Ravi Teja", "ravi.example.com", 3);
        click(reg.registerButton());
        System.out.println("[invalid]   bad e-mail                  -> " + reg.statusText());
        shot("t6_03_invalid_email.png");

        fill("192319010", "Ravi Teja", "ravi@example.com", 3);
        click(reg.registerButton());
        System.out.println("[normal]    valid form                  -> " + reg.statusText());
        shot("t6_04_normal_registration.png");

        fill("192319010", "Ravi Teja", "ravi@example.com", 3);
        click(reg.registerButton());
        System.out.println("[duplicate] same student, same event   -> " + reg.statusText());
        shot("t6_05_duplicate.png");

        SwingUtilities.invokeAndWait(() -> frame.showScreen(MainFrame.LIST));
        waitIdle();
        System.out.println("[table]     Registrations screen: " + list.pageLabel().getText() + ", " + list.statusLabel().getText());
        printTable();
        shot("t6_06_registrations_page1.png");

        click(list.nextButton());
        System.out.println("[paging]    Next -> " + list.pageLabel().getText());
        printTable();
        shot("t6_07_registrations_page2.png");

        SwingUtilities.invokeAndWait(() -> list.filterBox().setSelectedItem(service.events().get(1)));   // Cultural Night
        waitIdle();
        System.out.println("[filter]    event = Cultural Night -> " + list.pageLabel().getText() + ", " + list.statusLabel().getText());
        printTable();
        shot("t6_08_filter_cultural_night.png");

        // responsiveness: the same screen with a slow database (every call waits 400 ms)
        System.out.println();
        System.out.println("=== Responsiveness with a slow database (400 ms per call, count + page = 2 calls) ===");
        InMemoryRegistrationDao slowDao = InMemoryRegistrationDao.withDemoData();
        slowDao.setLatency(400);
        RegistrationService slow = new RegistrationService(slowDao);
        final RegistrationsPanel[] holder = new RegistrationsPanel[1];
        SwingUtilities.invokeAndWait(() -> { holder[0] = new RegistrationsPanel(slow, 5); holder[0].setEvents(java.util.List.of()); });
        RegistrationsPanel slowPanel = holder[0];
        long start = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> slowPanel.load(1));
        int pings = 0; long worst = 0; String seen = "";
        while (slowPanel.isLoading()) {
            long t = System.nanoTime();
            SwingUtilities.invokeAndWait(() -> { });
            worst = Math.max(worst, System.nanoTime() - t); pings++;
            seen = slowPanel.statusLabel().getText();
            Thread.sleep(40);
        }
        HeadlessRender.edtIdle();
        System.out.printf("page loaded after %d ms; while loading the status showed \"%s\"; %d pings to the event-dispatch thread, slowest answer %.2f ms%n",
                (System.nanoTime() - start) / 1000000, seen, pings, worst / 1e6);
        System.out.println("final status: " + slowPanel.statusLabel().getText() + " / " + slowPanel.pageLabel().getText());
    }
}
