import java.lang.management.*;

/** Student B: two transfers that ALWAYS deadlock, detected by the JVM's own deadlock detector. */
public class DeadlockDemo {
    public static void main(String[] args) throws Exception {
        Wallet a = new Wallet(1, 1000), b = new Wallet(2, 1000);
        Thread t1 = new Thread(() -> Wallet.transferUnsafe(a, b, 100, 200), "Transfer-1 (A -> B)");
        Thread t2 = new Thread(() -> Wallet.transferUnsafe(b, a, 50, 200), "Transfer-2 (B -> A)");
        t1.start(); t2.start();
        Thread.sleep(1000);
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) { System.out.println("no deadlock found"); return; }
        System.out.println("DEADLOCK DETECTED among " + ids.length + " threads:");
        for (ThreadInfo ti : mx.getThreadInfo(ids)) {
            System.out.println("  thread  : " + ti.getThreadName() + "   state = " + ti.getThreadState());
            System.out.println("    waiting for lock : " + ti.getLockName());
            System.out.println("    which is held by : " + ti.getLockOwnerName());
            for (MonitorInfo mi : ti.getLockedMonitors()) System.out.println("    it already holds : " + mi);
        }
        System.out.println("balances unchanged: A=" + a.balance + " B=" + b.balance + "  (neither transfer completed; both threads wait forever)");
        System.out.println("t1 alive=" + t1.isAlive() + " t2 alive=" + t2.isAlive() + " -> exiting the JVM with System.exit(0)");
        System.exit(0);
    }
}
