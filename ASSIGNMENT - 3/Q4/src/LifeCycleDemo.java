/** Student A: one thread is observed in every Thread.State. */
public class LifeCycleDemo {
    static final Object LOCK = new Object();
    static final Object MONITOR = new Object();

    static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }

    public static void main(String[] args) throws Exception {
        // NEW -> RUNNABLE -> TIMED_WAITING -> TERMINATED
        Thread t = new Thread(() -> { long end = System.nanoTime() + 150_000_000L; while (System.nanoTime() < end) { } pause(300); }, "worker");   // 150 ms busy, then sleep
        System.out.println("after new Thread(...)          : " + t.getState() + "   isAlive=" + t.isAlive());
        t.start();
        pause(50);
        System.out.println("50 ms after start(), computing : " + t.getState() + "   isAlive=" + t.isAlive());
        pause(250);
        System.out.println("while inside Thread.sleep(300) : " + t.getState());
        t.join();
        System.out.println("after the run() method ended   : " + t.getState() + "   isAlive=" + t.isAlive());

        // BLOCKED: another thread holds the monitor
        Thread blocked = new Thread(() -> { synchronized (LOCK) { } }, "blocked-one");
        synchronized (LOCK) {
            blocked.start();
            pause(150);
            System.out.println("waiting to enter synchronized  : " + blocked.getState());
        }
        blocked.join();

        // WAITING (join with no timeout / wait()) and TIMED_WAITING (wait with timeout)
        Thread waiting = new Thread(() -> { synchronized (MONITOR) { try { MONITOR.wait(); } catch (InterruptedException e) { } } }, "waiting-one");
        waiting.start(); pause(150);
        System.out.println("inside wait()                  : " + waiting.getState());
        synchronized (MONITOR) { MONITOR.notifyAll(); }
        waiting.join();
        Thread joiner = new Thread(() -> { try { Thread t2 = new Thread(() -> pause(300)); t2.start(); t2.join(); } catch (InterruptedException e) { } }, "joiner");
        joiner.start(); pause(100);
        System.out.println("inside join() of another thread: " + joiner.getState());
        joiner.join();

        System.out.println();
        System.out.println("A scanner station (Thread / Runnable) passes through: NEW -> RUNNABLE -> TIMED_WAITING (each sleep) <-> RUNNABLE -> TERMINATED.");
        System.out.println("main passes through WAITING while it is inside join().");
    }
}
