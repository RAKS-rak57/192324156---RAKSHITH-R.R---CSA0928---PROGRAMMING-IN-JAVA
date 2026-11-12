/** Pretends to call the bank. The delay replaces the junior developer's Thread.sleep(3000). */
public class SimulatedBank implements BankGateway {
    private final long delayMillis;
    public SimulatedBank(long delayMillis) { this.delayMillis = delayMillis; }

    @Override
    public boolean confirm(String regNo, double amount) throws InterruptedException {
        if (delayMillis > 0) Thread.sleep(delayMillis);
        return true;
    }
}
