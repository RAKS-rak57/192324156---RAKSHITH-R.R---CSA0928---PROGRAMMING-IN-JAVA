import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Every booking strategy behind one interface, so they can be tested and timed in the same way. */
public class Bookers {
    interface Booker { boolean book(int seat); int sold(); String name(); }

    static volatile long sink;
    /** Work that does NOT touch shared state (price lookup, payment, SMS). */
    static void externalWork(int micros) {
        if (micros > 0) { try { Thread.sleep(0, micros * 1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }

    /** No synchronization. `widen` inserts Thread.yield() between check and set to make the race window visible. */
    static class Unsync implements Booker {
        final boolean[] booked; int sold; final boolean widen;
        Unsync(int seats, boolean widen) { booked = new boolean[seats]; this.widen = widen; }
        public boolean book(int seat) {
            if (!booked[seat]) { if (widen) Thread.yield(); booked[seat] = true; sold++; return true; }
            return false;
        }
        public int sold() { return sold; }
        public String name() { return widen ? "unsynchronized (widened window)" : "unsynchronized"; }
    }
    /** synchronized METHOD: the external work is done while holding the lock. */
    static class SyncMethod implements Booker {
        final boolean[] booked; int sold; final int work;
        SyncMethod(int seats, int work) { booked = new boolean[seats]; this.work = work; }
        public synchronized boolean book(int seat) {
            externalWork(work);                                     // inside the lock
            if (!booked[seat]) { booked[seat] = true; sold++; return true; }
            return false;
        }
        public synchronized int sold() { return sold; }
        public String name() { return "synchronized method"; }
    }
    /** synchronized BLOCK: only check-and-set is locked. */
    static class SyncBlock implements Booker {
        final boolean[] booked; int sold; final int work; final Object lock = new Object();
        SyncBlock(int seats, int work) { booked = new boolean[seats]; this.work = work; }
        public boolean book(int seat) {
            externalWork(work);                                     // outside the lock
            synchronized (lock) {                                   // CRITICAL SECTION: check-then-act on shared data
                if (!booked[seat]) { booked[seat] = true; sold++; return true; }
                return false;
            }
        }
        public int sold() { synchronized (lock) { return sold; } }
        public String name() { return "synchronized block"; }
    }
    static class LockBooker implements Booker {
        final boolean[] booked; int sold; final int work; final ReentrantLock lock = new ReentrantLock();
        LockBooker(int seats, int work) { booked = new boolean[seats]; this.work = work; }
        public boolean book(int seat) {
            externalWork(work);
            lock.lock();
            try { if (!booked[seat]) { booked[seat] = true; sold++; return true; } return false; }
            finally { lock.unlock(); }
        }
        public int sold() { lock.lock(); try { return sold; } finally { lock.unlock(); } }
        public String name() { return "ReentrantLock"; }
    }
    static class AtomicBooker implements Booker {
        final AtomicBoolean[] booked; final AtomicInteger sold = new AtomicInteger(); final int work;
        AtomicBooker(int seats, int work) { booked = new AtomicBoolean[seats]; for (int i = 0; i < seats; i++) booked[i] = new AtomicBoolean(); this.work = work; }
        public boolean book(int seat) {
            externalWork(work);
            if (booked[seat].compareAndSet(false, true)) { sold.incrementAndGet(); return true; }   // atomic check-then-set
            return false;
        }
        public int sold() { return sold.get(); }
        public String name() { return "AtomicBoolean[] + compareAndSet"; }
    }
    static class MapBooker implements Booker {
        final ConcurrentHashMap<Integer, Boolean> map = new ConcurrentHashMap<>(); final int work;
        MapBooker(int seats, int work) { this.work = work; }
        public boolean book(int seat) {
            externalWork(work);
            return map.putIfAbsent(seat, Boolean.TRUE) == null;     // null means "I was first"
        }
        public int sold() { return map.size(); }
        public String name() { return "ConcurrentHashMap.putIfAbsent"; }
    }
}
