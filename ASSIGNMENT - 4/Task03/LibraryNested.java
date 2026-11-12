import java.awt.*;
import javax.swing.*;

/**
 * DESIGN A (chosen): nested BorderLayout + FlowLayout panels.
 *
 *   this (BorderLayout)
 *     NORTH  : north  (BorderLayout)  -> NORTH: title,  CENTER: searchRow (FlowLayout LEFT)
 *     CENTER : results (JScrollPane)                   <- gets ALL the extra space
 *     SOUTH  : south  (BorderLayout)  -> CENTER: buttonRow (FlowLayout CENTER),  SOUTH: status
 */
public class LibraryNested extends LibraryScreen {
    public LibraryNested() {
        setLayout(new BorderLayout(0, 4));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchRow.add(new JLabel("Search:"));
        searchRow.add(query);
        searchRow.add(category);
        searchRow.add(search);

        JPanel north = new JPanel(new BorderLayout());
        north.add(title, BorderLayout.NORTH);
        north.add(searchRow, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttonRow.add(prev);
        buttonRow.add(next);
        buttonRow.add(reserve);

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttonRow, BorderLayout.CENTER);
        south.add(status, BorderLayout.SOUTH);
        status.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));

        add(north, BorderLayout.NORTH);
        add(resultsScroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }
}
