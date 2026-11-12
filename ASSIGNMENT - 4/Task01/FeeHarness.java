import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/** Headless demonstration: drives the real FeePanel with button clicks and renders it. Not part of the application. */
public class FeeHarness {
    static void waitWhileBusy(FeePanel p) throws Exception {
        long t0 = System.currentTimeMillis();
        while (p.isBusy() && System.currentTimeMillis() - t0 < 10000) Thread.sleep(20);
        HeadlessRender.edtIdle();
    }

    public static void main(String[] args) throws Exception {
        String dir = "../screenshots/task01/";
        InMemoryPaymentRepository repo = new InMemoryPaymentRepository();
        FeeService service = new FeeService(repo, new SimulatedBank(1500));      // 1.5 s bank delay
        final FeePanel[] holder = new FeePanel[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new FeePanel(service));
        FeePanel panel = holder[0];
        panel.setSize(430, 300);

        HeadlessRender.save(HeadlessRender.render(panel, 430, 300, "Student Fee Payment"), dir + "t1_01_initial.png");
        System.out.println("[1] window opened: status=\"" + panel.statusText() + "\"");

        panel.payAction().actionPerformed(new java.awt.event.ActionEvent(panel, 0, "Pay"));   // like clicking Pay with empty fields
        waitWhileBusy(panel);
        System.out.println("[2] Pay with empty fields -> status: " + panel.statusText());
        HeadlessRender.save(HeadlessRender.render(panel, 430, 300, "Student Fee Payment"), dir + "t1_02_empty_error.png");

        panel.regNoField().setText("192311001");
        panel.amountField().setText("abc");
        panel.payAction().actionPerformed(new java.awt.event.ActionEvent(panel, 0, "Pay"));
        waitWhileBusy(panel);
        System.out.println("[3] Pay with amount 'abc'   -> status: " + panel.statusText());
        HeadlessRender.save(HeadlessRender.render(panel, 430, 300, "Student Fee Payment"), dir + "t1_03_invalid_amount.png");

        panel.amountField().setText("15000");
        long start = System.currentTimeMillis();
        panel.payAction().actionPerformed(new java.awt.event.ActionEvent(panel, 0, "Pay"));
        // while the bank call is running, ask the event-dispatch thread to do something small and time the answer
        long worst = 0; int pings = 0;
        while (panel.isBusy()) {
            long t = System.nanoTime();
            SwingUtilities.invokeAndWait(() -> { });
            worst = Math.max(worst, System.nanoTime() - t); pings++;
            Thread.sleep(50);
        }
        HeadlessRender.edtIdle();
        long total = System.currentTimeMillis() - start;
        System.out.println("[4] Pay 15000 (bank takes 1.5 s): finished after " + total + " ms; status: " + panel.statusText());
        System.out.printf("    event-dispatch thread stayed responsive: %d pings during the payment, slowest answer %.2f ms%n", pings, worst / 1e6);
        HeadlessRender.save(HeadlessRender.render(panel, 430, 300, "Student Fee Payment"), dir + "t1_04_success.png");

        panel.clearAction().actionPerformed(new java.awt.event.ActionEvent(panel, 0, "Clear"));
        panel.regNoField().setText("192311001");
        panel.receiptAction().actionPerformed(new java.awt.event.ActionEvent(panel, 0, "Receipt"));
        waitWhileBusy(panel);
        System.out.println("[5] Clear, then Receipt for 192311001 -> receipt area:");
        System.out.println("    " + panel.receiptText().replace("\n", "\n    "));
        HeadlessRender.save(HeadlessRender.render(panel, 430, 300, "Student Fee Payment"), dir + "t1_05_receipt.png");
    }
}
