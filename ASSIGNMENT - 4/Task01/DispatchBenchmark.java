import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Alternative concept: one listener with an if-else chain on the action command  versus
 * one listener object per button.  Measures dispatch time (ns per event) for k buttons and the memory for the listeners.
 */
public class DispatchBenchmark {
    static long sink = 0;

    static long allocated() {
        com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        return b.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    // one shared listener with an if-else chain (written as a loop over the commands: same k comparisons)
    static class ChainListener implements ActionListener {
        final String[] commands;
        ChainListener(String[] commands) { this.commands = commands; }
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            for (int i = 0; i < commands.length; i++) {
                if (commands[i].equals(cmd)) { sink += i; return; }        // "if (cmd.equals(...)) { ... }"
            }
        }
    }

    // each button has its own listener object: the event goes straight to the right one
    static class OwnListener implements ActionListener {
        final int id;
        OwnListener(int id) { this.id = id; }
        public void actionPerformed(ActionEvent e) { sink += id; }
    }

    public static void main(String[] args) throws Exception {
        int[] ks = {3, 10, 100, 1000, 10000};
        final int EVENTS = 200000;
        PrintWriter csv = new PrintWriter("dispatch_results.csv");
        csv.println("k,chain_ns_per_event,map_ns_per_event,own_listener_ns_per_event,chain_listener_bytes,own_listener_bytes");
        System.out.println("=== Dispatch time for the LAST button (worst case), ns per event, average of 5 runs ===");
        System.out.printf("%8s %14s %14s %16s %16s %16s%n", "k", "if-else chain", "HashMap", "own listener", "bytes: 1 listener", "bytes: k listeners");

        Object src = new Object();
        for (int w = 0; w < 3; w++) run(100, EVENTS / 4, src);        // warm-up
        for (int k : ks) {
            double chain = 0, map = 0, own = 0;
            for (int r = 0; r < 5; r++) { double[] t = run(k, EVENTS, src); chain += t[0]; map += t[1]; own += t[2]; }
            chain /= 5; map /= 5; own /= 5;

            // memory: one chain listener (+ the command array) against k separate listener objects
            String[] cmds = new String[k];
            for (int i = 0; i < k; i++) cmds[i] = "cmd" + i;
            long a0 = allocated();
            ActionListener one = new ChainListener(cmds);
            long oneBytes = allocated() - a0;
            a0 = allocated();
            ActionListener[] many = new ActionListener[k];
            for (int i = 0; i < k; i++) many[i] = new OwnListener(i);
            long manyBytes = allocated() - a0;
            sink += one.hashCode() + many.length;

            System.out.printf("%8d %14.1f %14.1f %16.1f %16d %16d%n", k, chain, map, own, oneBytes, manyBytes);
            csv.printf("%d,%.1f,%.1f,%.1f,%d,%d%n", k, chain, map, own, oneBytes, manyBytes);
        }
        csv.close();
        System.out.println("(checksum " + sink + ")");
    }

    // returns {chain, map, own} nanoseconds per event
    static double[] run(int k, int events, Object src) {
        String[] cmds = new String[k];
        ActionListener[] own = new ActionListener[k];
        Map<String, ActionListener> byName = new HashMap<>();
        for (int i = 0; i < k; i++) {
            cmds[i] = "cmd" + i;
            own[i] = new OwnListener(i);
            byName.put(cmds[i], own[i]);
        }
        ActionListener chain = new ChainListener(cmds);
        String last = "cmd" + (k - 1);
        ActionEvent ev = new ActionEvent(src, ActionEvent.ACTION_PERFORMED, last);

        long s = System.nanoTime();
        for (int i = 0; i < events; i++) chain.actionPerformed(ev);
        double t1 = (System.nanoTime() - s) / (double) events;

        s = System.nanoTime();
        for (int i = 0; i < events; i++) byName.get(ev.getActionCommand()).actionPerformed(ev);
        double t2 = (System.nanoTime() - s) / (double) events;

        ActionListener direct = own[k - 1];                    // a button already holds its own listener
        s = System.nanoTime();
        for (int i = 0; i < events; i++) direct.actionPerformed(ev);
        double t3 = (System.nanoTime() - s) / (double) events;
        return new double[]{t1, t2, t3};
    }
}
