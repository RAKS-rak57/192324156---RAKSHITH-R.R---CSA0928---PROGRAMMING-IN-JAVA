import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * The view: it only collects input and shows results. Each button has its OWN Action object
 * (no if-else chain on the action command). All slow work is done by SwingWorker, off the event-dispatch thread.
 */
public class FeePanel extends JPanel {
    private final FeeService service;
    private final JTextField regNoField = new JTextField(14);
    private final JTextField amountField = new JTextField(14);
    private final JLabel status = new JLabel(" ");
    private final JTextArea receiptArea = new JTextArea(6, 32);
    private final Action payAction = new PayAction();
    private final Action clearAction = new ClearAction();
    private final Action receiptAction = new ReceiptAction();
    private volatile boolean busy = false;

    public FeePanel(FeeService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
        form.add(new JLabel("Register number:")); form.add(regNoField);
        form.add(new JLabel("Amount (Rs.):"));    form.add(amountField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        buttons.add(new JButton(payAction));
        buttons.add(new JButton(clearAction));
        buttons.add(new JButton(receiptAction));

        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(form, BorderLayout.CENTER);
        north.add(buttons, BorderLayout.SOUTH);

        receiptArea.setEditable(false);
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(receiptArea), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    public boolean isBusy() { return busy; }
    public String statusText() { return status.getText(); }
    public String receiptText() { return receiptArea.getText(); }
    public JTextField regNoField() { return regNoField; }
    public JTextField amountField() { return amountField; }
    public Action payAction() { return payAction; }
    public Action clearAction() { return clearAction; }
    public Action receiptAction() { return receiptAction; }

    private void showStatus(String text, boolean error) {
        status.setForeground(error ? new Color(0xB00020) : new Color(0x1B5E20));
        status.setText(text);
    }

    private void setBusy(boolean b) {
        busy = b;
        payAction.setEnabled(!b);
        receiptAction.setEnabled(!b);
    }

    /** Runs slow work in a worker thread and delivers the result on the event-dispatch thread. */
    private <T> void runInBackground(Callable<T> work, Consumer<T> onDone) {
        setBusy(true);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                try {
                    onDone.accept(get());
                } catch (InterruptedException | ExecutionException ex) {
                    showStatus("Unexpected error: " + ex.getMessage(), true);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private class PayAction extends AbstractAction {
        PayAction() { super("Pay"); putValue(MNEMONIC_KEY, KeyEvent.VK_P); }
        @Override public void actionPerformed(ActionEvent e) {
            final String reg = regNoField.getText(), amt = amountField.getText();
            showStatus("Contacting the bank ...", false);
            runInBackground(() -> service.pay(reg, amt), result -> {
                showStatus(result.message, !result.success);
                if (result.success) receiptArea.setText(result.receiptText);
            });
        }
    }

    private class ClearAction extends AbstractAction {
        ClearAction() { super("Clear"); putValue(MNEMONIC_KEY, KeyEvent.VK_C); }
        @Override public void actionPerformed(ActionEvent e) {
            regNoField.setText(""); amountField.setText(""); receiptArea.setText(""); showStatus(" ", false);
            regNoField.requestFocusInWindow();
        }
    }

    private class ReceiptAction extends AbstractAction {
        ReceiptAction() { super("Receipt"); putValue(MNEMONIC_KEY, KeyEvent.VK_R); }
        @Override public void actionPerformed(ActionEvent e) {
            final String reg = regNoField.getText();
            runInBackground(() -> service.receipt(reg), text -> { receiptArea.setText(text); showStatus("Receipt list updated.", false); });
        }
    }
}
