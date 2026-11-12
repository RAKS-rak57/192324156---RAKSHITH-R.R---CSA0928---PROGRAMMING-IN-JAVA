import java.awt.FlowLayout;
import java.awt.event.*;
import javax.swing.*;

/** Alternative (f): ONE shared Action used by the button, a menu item and the Enter key. A new trigger = one line. */
public class ConverterF extends JFrame {
    private final JTextField amount = new JTextField(10);
    private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    private final JLabel result = new JLabel(" ");
    private final JButton convertButton = new JButton("Convert");

    private static double rate(String currency) { return currency.equals("USD") ? 0.012 : 0.011; }
    private final JMenuItem convertMenuItem;

    /** The Action carries the name, the keyboard shortcut and the code: every trigger shares them. */
    private class ConvertAction extends AbstractAction {
        ConvertAction() {
            super("Convert");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                double inr = Double.parseDouble(amount.getText().trim());
                if (Double.isNaN(inr) || Double.isInfinite(inr)) throw new NumberFormatException();
                String currency = (String) target.getSelectedItem();
                String text = String.format("%.2f %s", inr * rate(currency), currency);
                result.setText(text);
                JOptionPane.showMessageDialog(ConverterF.this, text, "Result", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                result.setText("Invalid amount");
                JOptionPane.showMessageDialog(ConverterF.this, "Invalid amount", "Result", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public ConverterF() {
        super("Style (f): shared Action");
        setLayout(new FlowLayout());
        Action convertAction = new ConvertAction();
        convertButton.setAction(convertAction);                          // trigger 1: button
        amount.addActionListener(convertAction);                         // trigger 2: Enter key in the field
        JMenu menu = new JMenu("Tools");
        convertMenuItem = new JMenuItem(convertAction);                  // trigger 3: menu item (with Ctrl+Enter shown)
        menu.add(convertMenuItem);
        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        setJMenuBar(bar);
        add(new JLabel("INR")); add(amount); add(target); add(convertButton); add(result);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });
        pack();
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new ConverterF().setVisible(true)); }
}
