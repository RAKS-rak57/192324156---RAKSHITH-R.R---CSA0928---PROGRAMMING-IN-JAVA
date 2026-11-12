import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** Common widgets and behaviour of the search screen. The three subclasses differ ONLY in their layout. */
public abstract class LibraryScreen extends JPanel {
    protected final JLabel title = new JLabel("Campus Library Search", SwingConstants.CENTER);
    protected final JTextField query = new JTextField(20);
    protected final JComboBox<String> category = new JComboBox<>(new String[]{"All", "Title", "Author", "Subject", "ISBN"});
    protected final JButton search = new JButton("Search");
    protected final JTextArea results = new JTextArea();
    protected final JScrollPane resultsScroll = new JScrollPane(results);
    protected final JButton prev = new JButton("Prev");
    protected final JButton next = new JButton("Next");
    protected final JButton reserve = new JButton("Reserve");
    protected final JLabel status = new JLabel("Ready");

    private static final int PAGE = 5;
    private List<String> found = new ArrayList<>();
    private int page = 0;

    protected LibraryScreen() {
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        title.setOpaque(true);
        title.setBackground(new java.awt.Color(0x2F5496));
        title.setForeground(java.awt.Color.WHITE);
        results.setEditable(false);
        search.addActionListener(this::doSearch);
        query.addActionListener(this::doSearch);
        prev.addActionListener(e -> { if (page > 0) { page--; showPage(); } });
        next.addActionListener(e -> { if ((page + 1) * PAGE < found.size()) { page++; showPage(); } });
        reserve.addActionListener(e -> status.setText("Reserve requested for page " + (page + 1)));
    }

    private void doSearch(ActionEvent e) {
        found = BookCatalog.search((String) category.getSelectedItem(), query.getText());
        page = 0;
        showPage();
    }

    private void showPage() {
        StringBuilder sb = new StringBuilder();
        for (int i = page * PAGE; i < Math.min(found.size(), (page + 1) * PAGE); i++) sb.append(i + 1).append(". ").append(found.get(i)).append('\n');
        results.setText(sb.toString());
        status.setText(found.size() + " result(s) - page " + (page + 1) + " of " + Math.max(1, (found.size() + PAGE - 1) / PAGE));
    }

    // used by the tests
    public JButton searchButton() { return search; }
    public JTextField queryField() { return query; }
    public JScrollPane resultsPane() { return resultsScroll; }
    public JButton prevButton() { return prev; }
    public JButton nextButton() { return next; }
    public JButton reserveButton() { return reserve; }
    public JLabel statusLabel() { return status; }
    public JLabel titleLabel() { return title; }
    public JComboBox<String> categoryBox() { return category; }
}
