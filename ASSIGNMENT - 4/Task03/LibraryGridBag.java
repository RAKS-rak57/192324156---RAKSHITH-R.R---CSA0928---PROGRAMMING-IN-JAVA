import java.awt.*;
import javax.swing.*;

/** DESIGN B: one panel with GridBagLayout (weights and fill decide what grows). */
public class LibraryGridBag extends LibraryScreen {
    public LibraryGridBag() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);

        c.gridx = 0; c.gridy = 0; c.gridwidth = 4; c.weightx = 1; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        add(title, c);                                                     // title bar: stretches sideways only

        c.gridy = 1; c.gridwidth = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST;
        add(new JLabel("Search:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(query, c);                                                     // the text field takes the spare width
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        add(category, c);
        c.gridx = 3;
        add(search, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 4; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        add(resultsScroll, c);                                             // results: grows both ways

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));   // one small helper panel for the 3 buttons
        buttons.add(prev); buttons.add(next); buttons.add(reserve);
        c.gridy = 3; c.weighty = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.CENTER;
        add(buttons, c);

        c.gridy = 4; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.WEST;
        add(status, c);
    }
}
