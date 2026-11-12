import java.util.concurrent.locks.ReentrantLock;

/** A hostel wallet. Each transfer must lock BOTH wallets. */
public class Wallet {
    final int id; long balance; final ReentrantLock lock = new ReentrantLock();
    Wallet(int id, long balance) { this.id = id; this.balance = balance; }
    @Override public String toString() { return "Wallet" + id; }

    static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    /** DEADLOCK-PRONE: locks 'from' first, then 'to', in the order given by the caller. */
    static void transferUnsafe(Wallet from, Wallet to, long amount, long pauseMs) {
        synchronized (from) {
            pause(pauseMs);                       // makes the bad interleaving certain (a real system just needs bad luck)
            synchronized (to) { from.balance -= amount; to.balance += amount; }
        }
    }
    /** DEADLOCK-FREE: always lock the wallet with the SMALLER id first. */
    static void transferOrdered(Wallet from, Wallet to, long amount, long pauseMs) {
        Wallet first = from.id < to.id ? from : to, second = from.id < to.id ? to : from;
        synchronized (first) {
            pause(pauseMs);
            synchronized (second) { from.balance -= amount; to.balance += amount; }
        }
    }
    /** ALTERNATIVE: tryLock with a timeout; give up and retry if the second lock is not available. Returns the number of retries. */
    static int transferTryLock(Wallet from, Wallet to, long amount, long pauseMs) throws InterruptedException {
        int retries = 0;
        while (true) {
            if (from.lock.tryLock(5, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                try {
                    pause(pauseMs);
                    if (to.lock.tryLock(5, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        try { from.balance -= amount; to.balance += amount; return retries; }
                        finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }          // could not get the second lock: release the first, back off
            }
            retries++;
            Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 10));   // random back-off avoids live-lock
        }
    }
}
