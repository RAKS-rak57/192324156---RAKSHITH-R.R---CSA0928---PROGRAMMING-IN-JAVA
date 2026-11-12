import javax.swing.*;

/** Runs the chosen design in a real window:  java LibraryFrame [nested|gridbag|absolute] */
public class LibraryFrame extends JFrame {
    public LibraryFrame(LibraryScreen screen) {
        super("Campus Library Search");
        add(screen);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);
    }
    public static void main(String[] args) {
        String design = args.length > 0 ? args[0] : "nested";
        SwingUtilities.invokeLater(() -> {
            LibraryScreen s = design.equals("gridbag") ? new LibraryGridBag() : design.equals("absolute") ? new LibraryAbsolute() : new LibraryNested();
            new LibraryFrame(s).setVisible(true);
        });
    }
}
