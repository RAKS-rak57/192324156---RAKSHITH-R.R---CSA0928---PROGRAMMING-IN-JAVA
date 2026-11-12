import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** The appointment form (view + event handlers). All rules are in AppointmentService. */
public class AppointmentPanel extends JPanel {
    private final AppointmentService service;
    private final JTextField patientId = new JTextField(10);
    private final JComboBox<String> departmentBox = new JComboBox<>();
    private final JComboBox<Doctor> doctorBox = new JComboBox<>();
    private final JComboBox<String> slotBox = new JComboBox<>();
    private final JButton bookButton = new JButton("Book appointment");
    private final JLabel status = new JLabel(" ");
    private final List<String> trace = new ArrayList<>();         // one line per event handled (used in the report)
    private volatile boolean busy = false;

    public AppointmentPanel(AppointmentService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        departmentBox.addItem(AppointmentService.PROMPT);
        for (String d : service.departments()) departmentBox.addItem(d);
        slotBox.addItem(AppointmentService.PROMPT);
        for (String s : service.slots()) slotBox.addItem(s);

        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.add(new JLabel("Patient ID:"));  form.add(patientId);
        form.add(new JLabel("Department:"));  form.add(departmentBox);
        form.add(new JLabel("Doctor:"));      form.add(doctorBox);
        form.add(new JLabel("Time slot:"));   form.add(slotBox);
        JPanel south = new JPanel(new BorderLayout(4, 4));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonRow.add(bookButton);
        south.add(buttonRow, BorderLayout.NORTH);
        south.add(status, BorderLayout.SOUTH);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // ---- event sources and their listeners ----
        departmentBox.addItemListener(e -> {                                   // ItemEvent  -> ItemListener
            if (e.getStateChange() == ItemEvent.SELECTED) {                    // ignore the DESELECTED event of the old item
                trace.add("ItemEvent(SELECTED) from departmentBox -> " + e.getItem());
                refreshDoctors();
            }
        });
        bookButton.addActionListener(e -> {                                    // ActionEvent -> ActionListener
            trace.add("ActionEvent from bookButton");
            book();
        });
        patientId.getDocument().addDocumentListener(new DocumentListener() {   // DocumentEvent -> DocumentListener
            public void insertUpdate(DocumentEvent e)  { trace.add("DocumentEvent(insert) from patientId"); }
            public void removeUpdate(DocumentEvent e)  { trace.add("DocumentEvent(remove) from patientId"); }
            public void changedUpdate(DocumentEvent e) { }
        });
    }

    /** (a) refresh the doctor list when the department changes */
    private void refreshDoctors() {
        String dept = (String) departmentBox.getSelectedItem();
        doctorBox.removeAllItems();
        for (Doctor d : service.doctorsFor(dept)) doctorBox.addItem(d);        // empty list when the prompt is selected
        status.setText(" ");
    }

    /** (b) validate and book (the database work runs in a SwingWorker, not on the event-dispatch thread) */
    private void book() {
        final String id = patientId.getText();
        final String dept = (String) departmentBox.getSelectedItem();
        final Doctor doc = (Doctor) doctorBox.getSelectedItem();
        final String slot = (String) slotBox.getSelectedItem();
        busy = true;
        bookButton.setEnabled(false);
        new SwingWorker<BookingResult, Void>() {
            @Override protected BookingResult doInBackground() { return service.book(id, dept, doc, slot); }
            @Override protected void done() {
                try {
                    BookingResult r = get();
                    if (r.success) resetForm();             // reset first: it fires the department listener, which clears the status line
                    status.setForeground(r.success ? new Color(0x1B5E20) : new Color(0xB00020));
                    status.setText(r.message);
                } catch (InterruptedException | ExecutionException ex) {
                    status.setForeground(new Color(0xB00020));
                    status.setText("Unexpected error: " + ex.getMessage());
                } finally {
                    busy = false;
                    bookButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void resetForm() {
        patientId.setText("");
        departmentBox.setSelectedIndex(0);
        slotBox.setSelectedIndex(0);
    }

    /** True while a booking is being typed in or is being saved: the window then asks before closing. */
    public boolean isBookingInProgress() {
        return busy || !patientId.getText().trim().isEmpty()
                || departmentBox.getSelectedIndex() > 0 || slotBox.getSelectedIndex() > 0;
    }

    public boolean isBusy() { return busy; }
    public List<String> trace() { return trace; }
    public String statusText() { return status.getText(); }
    public JTextField patientIdField() { return patientId; }
    public JComboBox<String> departmentBox() { return departmentBox; }
    public JComboBox<Doctor> doctorBox() { return doctorBox; }
    public JComboBox<String> slotBox() { return slotBox; }
    public JButton bookButton() { return bookButton; }
}
