package campus.reg.ui;

import campus.reg.model.*;
import campus.reg.service.RegistrationService;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Screen 2: all registrations in a JTable, filter by event, one page at a time (SQL LIMIT / OFFSET inside a SwingWorker). */
public class RegistrationsPanel extends JPanel {
    private static final String[] COLUMNS = {"Reg ID", "Register No", "Student", "Event", "Registered on"};
    private final RegistrationService service;
    private final int pageSize;
    private final JComboBox<CampusEvent> filterBox = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton prevButton = new JButton("Prev");
    private final JButton nextButton = new JButton("Next");
    private final JButton refreshButton = new JButton("Refresh");
    private final JLabel pageLabel = new JLabel("Page 1 of 1");
    private final JLabel status = new JLabel(" ");
    private volatile boolean loading = false;
    private int currentPage = 1;
    private PageData lastPage;

    public RegistrationsPanel(RegistrationService service, int pageSize) {
        this.service = service;
        this.pageSize = pageSize;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Show event:")); top.add(filterBox); top.add(refreshButton);

        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        nav.add(prevButton); nav.add(pageLabel); nav.add(nextButton);
        bottom.add(nav, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);

        int[] widths = {55, 110, 140, 170, 150};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());       // column titles (normally installed automatically when the window is shown)

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        filterBox.addActionListener(e -> { if (!loading) load(1); });
        prevButton.addActionListener(e -> load(currentPage - 1));
        nextButton.addActionListener(e -> load(currentPage + 1));
        refreshButton.addActionListener(e -> load(currentPage));
    }

    public void setEvents(List<CampusEvent> events) {
        filterBox.removeAllItems();
        filterBox.addItem(CampusEvent.ALL);
        for (CampusEvent ev : events) filterBox.addItem(ev);
    }

    /** Starts loading page pageNo in the background; the table is updated on the event-dispatch thread when the data arrives. */
    public void load(int pageNo) {
        final CampusEvent filter = (CampusEvent) filterBox.getSelectedItem();
        loading = true;
        setControlsEnabled(false);
        status.setForeground(Color.DARK_GRAY);
        status.setText("Loading ...");
        new SwingWorker<PageData, Void>() {
            @Override protected PageData doInBackground() { return service.loadPage(filter, pageNo, pageSize); }   // JDBC call, off the EDT
            @Override protected void done() {
                try {
                    show(get());
                } catch (InterruptedException | ExecutionException ex) {
                    status.setForeground(new Color(0xB00020));
                    status.setText("Could not load: " + ex.getCause());
                } finally {
                    loading = false;
                    setControlsEnabled(true);
                    updateNavButtons();
                }
            }
        }.execute();
    }

    private void show(PageData data) {
        lastPage = data;
        currentPage = data.pageNo();
        model.setRowCount(0);
        for (RegistrationRow r : data.rows()) model.addRow(new Object[]{r.regId(), r.regNo(), r.studentName(), r.eventTitle(), r.registeredOn()});
        pageLabel.setText("Page " + data.pageNo() + " of " + data.pageCount());
        status.setText(data.totalRows() + " registration(s) found");
    }

    private void setControlsEnabled(boolean on) { filterBox.setEnabled(on); refreshButton.setEnabled(on); prevButton.setEnabled(on); nextButton.setEnabled(on); }
    private void updateNavButtons() { if (lastPage != null) { prevButton.setEnabled(lastPage.hasPrev()); nextButton.setEnabled(lastPage.hasNext()); } }

    public CampusEvent selectedFilter() { return (CampusEvent) filterBox.getSelectedItem(); }
    public boolean isLoading() { return loading; }
    public JTable table() { return table; }
    public JComboBox<CampusEvent> filterBox() { return filterBox; }
    public JButton prevButton() { return prevButton; }
    public JButton nextButton() { return nextButton; }
    public JLabel pageLabel() { return pageLabel; }
    public JLabel statusLabel() { return status; }
}
