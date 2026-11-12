import java.awt.FlowLayout;
import java.awt.event.*;
import javax.swing.*;

/** Style (d): lambda expression */
public class ConverterD2 extends JFrame {
    private final JTextField amount = new JTextField(10);
    private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    private final JLabel result = new JLabel(" ");
    private final JButton convertButton = new JButton("Convert");

    private static double rate(String currency) { return currency.equals("USD") ? 0.012 : 0.011; }

    public ConverterD2() {
        super("Style (d): lambda expression");
        setLayout(new FlowLayout());
        add(new JLabel("INR")); add(amount); add(target); add(convertButton); add(result);
        ActionListener convertListener = e -> convert();
        convertButton.addActionListener(convertListener);
        amount.addActionListener(convertListener);
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

    private void convert() {
        try {
            double inr = Double.parseDouble(amount.getText().trim());
            if (Double.isNaN(inr) || Double.isInfinite(inr)) throw new NumberFormatException();
            String currency = (String) target.getSelectedItem();
            String text = String.format("%.2f %s", inr * rate(currency), currency);
            result.setText(text);
            JOptionPane.showMessageDialog(this, text, "Result", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            result.setText("Invalid amount");
            JOptionPane.showMessageDialog(this, "Invalid amount", "Result", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConverterD2().setVisible(true));
    }
}
