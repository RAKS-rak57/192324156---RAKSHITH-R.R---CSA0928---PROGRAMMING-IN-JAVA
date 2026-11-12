/** A generic bounded counter used for stress tests: capacity, notify/notifyAll and while/if are switches. */
public class BoundedCounter {
    private final int[] buffer; private int count = 0, in = 0, out = 0;
    private final boolean useNotifyAll, useWhile;
    int minSeen = Integer.MAX_VALUE, maxSeen = Integer.MIN_VALUE;           // size monitoring
    final java.util.List<Integer> trace = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    boolean record = false;

    BoundedCounter(int capacity, boolean useNotifyAll, boolean useWhile) { buffer = new int[capacity]; this.useNotifyAll = useNotifyAll; this.useWhile = useWhile; }

    private void note() { minSeen = Math.min(minSeen, count); maxSeen = Math.max(maxSeen, count); if (record) trace.add(count); }
    private void wake() { if (useNotifyAll) notifyAll(); else notify(); }

    public synchronized void put(int token) throws InterruptedException {
        if (useWhile) { while (count == buffer.length) wait(); } else { if (count == buffer.length) wait(); }
        buffer[in] = token; in = (in + 1) % buffer.length; count++; note(); wake();
    }
    public synchronized int take() throws InterruptedException {
        if (useWhile) { while (count == 0) wait(); } else { if (count == 0) wait(); }
        int t = buffer[out]; out = (out + 1) % buffer.length; count--; note(); wake(); return t;
    }
    public synchronized int size() { return count; }
}
