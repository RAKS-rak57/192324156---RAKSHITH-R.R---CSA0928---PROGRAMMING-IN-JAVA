package campus.reg.ui;

import campus.reg.service.RegistrationService;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import javax.swing.*;

/** The main window: menu bar + two screens (cards). Only wiring and menu actions live here. */
public class MainFrame extends JFrame {
    public static final String REGISTER = "register", LIST = "list";
    private final RegistrationService service;
    private final RegisterPanel registerPanel;
    private final RegistrationsPanel listPanel;
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JMenuBar menuBar = new JMenuBar();

    public MainFrame(RegistrationService service, int pageSize) {
        super("Campus Event Registration");
        this.service = service;
        registerPanel = new RegisterPanel(service);
        listPanel = new RegistrationsPanel(service, pageSize);
        registerPanel.setEvents(service.events());
        listPanel.setEvents(service.events());
        registerPanel.setOnRegistered(() -> { if (!listPanel.isLoading()) listPanel.load(1); });   // keep the table up to date

        center.add(registerPanel, REGISTER);
        center.add(listPanel, LIST);
        add(center, BorderLayout.CENTER);
        buildMenu();
        setJMenuBar(menuBar);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { requestExit(); }
        });
        setSize(620, 420);
        setLocationRelativeTo(null);
        showScreen(REGISTER);
    }

    /** Shared by the window's close button and File > Exit: asks only when work is still running. */
    void requestExit() {
        if (registerPanel.isBusy() || listPanel.isLoading()) {
            int a = JOptionPane.showConfirmDialog(this, "Work is still in progress. Exit anyway?", "Exit", JOptionPane.YES_NO_OPTION);
            if (a != JOptionPane.YES_OPTION) return;
        }
        dispose();
        System.exit(0);
    }

    private void buildMenu() {
        JMenu file = new JMenu("File");
        JMenuItem export = new JMenuItem("Export CSV...");
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        export.addActionListener(e -> exportCsv());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> requestExit());
        file.add(export); file.addSeparator(); file.add(exit);

        JMenu view = new JMenu("View");
        JMenuItem reg = new JMenuItem("Register screen");
        reg.addActionListener(e -> showScreen(REGISTER));
        JMenuItem list = new JMenuItem("Registrations screen");
        list.addActionListener(e -> showScreen(LIST));
        JMenuItem refresh = new JMenuItem("Refresh table");
        refresh.addActionListener(e -> { showScreen(LIST); });
        view.add(reg); view.add(list); view.addSeparator(); view.add(refresh);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Campus Event Registration\nCultural committee desktop application\nSwing + JDBC (MySQL)", "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        menuBar.add(file); menuBar.add(view); menuBar.add(help);
    }

    /** Menu action: ask for a file name, then write the CSV in a worker thread. */
    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("registrations.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        final Path path = chooser.getSelectedFile().toPath();
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return service.exportCsv(path, listPanel.selectedFilter()); }
            @Override protected void done() {
                try {
                    JOptionPane.showMessageDialog(MainFrame.this, get() + " row(s) exported to " + path, "Export", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Export failed: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void showScreen(String name) {
        cards.show(center, name);
        if (LIST.equals(name) && !listPanel.isLoading()) listPanel.load(1);
    }

    public RegisterPanel registerPanel() { return registerPanel; }
    public RegistrationsPanel listPanel() { return listPanel; }
    @Override public JMenuBar getJMenuBar() { return menuBar; }
}
