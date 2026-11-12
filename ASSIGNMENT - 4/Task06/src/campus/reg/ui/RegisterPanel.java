package campus.reg.ui;

import campus.reg.model.CampusEvent;
import campus.reg.service.RegistrationService;
import campus.reg.service.ServiceResult;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

/** Screen 1: the registration form. Collects input and shows the result. Saving runs in a SwingWorker. */
public class RegisterPanel extends JPanel {
    private final RegistrationService service;
    private final JTextField regNo = new JTextField(14);
    private final JTextField name = new JTextField(14);
    private final JTextField email = new JTextField(14);
    private final JComboBox<CampusEvent> eventBox = new JComboBox<>();
    private final JButton registerButton = new JButton("Register");
    private final JButton clearButton = new JButton("Clear");
    private final JLabel status = new JLabel(" ");
    private volatile boolean busy = false;
    private Runnable onRegistered = () -> { };

    public RegisterPanel(RegistrationService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel heading = new JLabel("Register for an event");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Register number:")); form.add(regNo);
        form.add(new JLabel("Student name:"));    form.add(name);
        form.add(new JLabel("E-mail:"));          form.add(email);
        form.add(new JLabel("Event:"));           form.add(eventBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        buttons.add(registerButton); buttons.add(clearButton);
        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.NORTH);
        south.add(status, BorderLayout.SOUTH);

        add(heading, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        registerButton.addActionListener(e -> register());
        clearButton.addActionListener(e -> clear());
    }

    public void setEvents(List<CampusEvent> events) {
        eventBox.removeAllItems();
        eventBox.addItem(CampusEvent.ALL);                    // the "select" prompt (id 0 is rejected by the service)
        for (CampusEvent ev : events) eventBox.addItem(ev);
        eventBox.getModel().setSelectedItem(CampusEvent.ALL);
    }

    public void setOnRegistered(Runnable r) { onRegistered = r; }

    private void register() {
        final String r = regNo.getText(), n = name.getText(), m = email.getText();
        final CampusEvent ev = (CampusEvent) eventBox.getSelectedItem();
        busy = true;
        registerButton.setEnabled(false);
        showStatus("Saving ...", false);
        new SwingWorker<ServiceResult, Void>() {
            @Override protected ServiceResult doInBackground() { return service.register(r, n, m, ev); }   // database work: NOT on the EDT
            @Override protected void done() {
                try {
                    ServiceResult result = get();
                    showStatus(result.message, !result.success);
                    if (result.success) { clearFields(); onRegistered.run(); }
                } catch (InterruptedException | ExecutionException ex) {
                    showStatus("Unexpected error: " + ex.getMessage(), true);
                } finally {
                    busy = false;
                    registerButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void clearFields() { regNo.setText(""); name.setText(""); email.setText(""); eventBox.setSelectedIndex(0); }
    private void clear() { clearFields(); showStatus(" ", false); }
    private void showStatus(String text, boolean error) {
        status.setForeground(error ? new Color(0xB00020) : new Color(0x1B5E20));
        status.setText(text);
    }

    public boolean isBusy() { return busy; }
    public String statusText() { return status.getText(); }
    public JTextField regNoField() { return regNo; }
    public JTextField nameField() { return name; }
    public JTextField emailField() { return email; }
    public JComboBox<CampusEvent> eventBox() { return eventBox; }
    public JButton registerButton() { return registerButton; }
}
