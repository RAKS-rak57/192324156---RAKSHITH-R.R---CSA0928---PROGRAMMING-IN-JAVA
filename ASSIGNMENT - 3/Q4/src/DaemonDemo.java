/** Student B: a daemon thread does not keep the JVM alive. */
public class DaemonDemo {
    public static void main(String[] args) throws Exception {
        boolean daemon = args.length > 0 && args[0].equals("daemon");
        Thread s4 = new Thread(() -> {
            for (int i = 1; i <= 20; i++) {
                System.out.println("   S4 scanned script " + i);
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
            }
            System.out.println("   S4 FINISHED its whole batch");
        }, "S4");
        s4.setDaemon(daemon);
        System.out.println("S4 isDaemon = " + s4.isDaemon());
        s4.start();
        Thread s1 = new Thread(() -> Scanner1.scan("S1", 20), "S1");
        s1.start(); s1.join();                                          // main only waits for S1 (about 50 ms)
        System.out.println("main() is ending now");
    }
}
