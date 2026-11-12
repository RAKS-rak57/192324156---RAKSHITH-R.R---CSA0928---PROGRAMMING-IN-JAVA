import javax.swing.*;

/** DESIGN C: absolute positioning (setLayout(null) + setBounds). Positions are fixed numbers, so nothing reacts to resizing. */
public class LibraryAbsolute extends LibraryScreen {
    public LibraryAbsolute() {
        setLayout(null);
        title.setBounds(0, 0, 600, 34);
        JLabel label = new JLabel("Search:");
        label.setBounds(10, 44, 55, 26);
        query.setBounds(70, 44, 250, 26);
        category.setBounds(330, 44, 100, 26);
        search.setBounds(440, 44, 90, 26);
        resultsScroll.setBounds(10, 80, 580, 240);
        prev.setBounds(150, 330, 90, 28);
        next.setBounds(255, 330, 90, 28);
        reserve.setBounds(360, 330, 90, 28);
        status.setBounds(10, 368, 580, 22);
        add(title); add(label); add(query); add(category); add(search);
        add(resultsScroll); add(prev); add(next); add(reserve); add(status);
    }
    @Override public java.awt.Dimension getPreferredSize() { return new java.awt.Dimension(600, 400); }
}
