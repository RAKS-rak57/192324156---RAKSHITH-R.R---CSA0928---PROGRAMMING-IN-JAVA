import java.util.concurrent.atomic.AtomicInteger;

/** Student B: instance lock vs class lock, and which monitor each synchronized form uses. */
public class StaticSyncDemo {
    static int globalSold = 0;                  // ONE counter shared by all screens (static)
    static final Object PRIVATE = new Object();

    // Each screen has its OWN monitor (this) - it does NOT protect the shared static counter.
    synchronized void recordWrong() { int t = globalSold; Thread.yield(); globalSold = t + 1; }   // yield widens the read-modify-write window
    // The monitor is the Class object: ONE lock for every screen - protects the static counter.
    static synchronized void recordRight() { int t = globalSold; Thread.yield(); globalSold = t + 1; }

    synchronized void method()  { System.out.println("  synchronized method      holds this lock         : " + Thread.holdsLock(this) + "   holds class lock: " + Thread.holdsLock(StaticSyncDemo.class)); }
    void block()                { synchronized (this) { System.out.println("  synchronized (this)      holds this lock         : " + Thread.holdsLock(this)); } }
    void privateBlock()         { synchronized (PRIVATE) { System.out.println("  synchronized (private)   holds the private lock  : " + Thread.holdsLock(PRIVATE) + "   holds this lock: " + Thread.holdsLock(this)); } }
    static synchronized void staticMethod() { System.out.println("  static synchronized      holds the Class lock    : " + Thread.holdsLock(StaticSyncDemo.class)); }

    static int hammer(boolean right) throws Exception {
        globalSold = 0;
        int screens = 4, per = 20_000;
        Thread[] ts = new Thread[screens];
        for (int i = 0; i < screens; i++) {
            final StaticSyncDemo screen = new StaticSyncDemo();      // 4 different screens = 4 different monitors
            ts[i] = new Thread(() -> { for (int k = 0; k < per; k++) { if (right) recordRight(); else screen.recordWrong(); } });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        return globalSold;
    }
    public static void main(String[] args) throws Exception {
        System.out.println("=== Which intrinsic lock does each form use? ===");
        StaticSyncDemo s = new StaticSyncDemo();
        s.method(); s.block(); s.privateBlock(); staticMethod();

        System.out.println();
        System.out.println("=== 4 screens, each booking 20,000 times into ONE static counter (expected 80000) ===");
        for (int rep = 1; rep <= 3; rep++) System.out.println("  instance-synchronized (4 different locks): " + hammer(false));
        for (int rep = 1; rep <= 3; rep++) System.out.println("  static synchronized   (one Class lock)   : " + hammer(true));
        System.out.println("(Thread.yield() between the read and the write of the counter widens the window so the lost updates are visible on one core)");
    }
}
