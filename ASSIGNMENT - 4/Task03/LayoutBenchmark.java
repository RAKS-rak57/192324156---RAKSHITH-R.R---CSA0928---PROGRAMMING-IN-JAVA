import java.awt.*;
import java.io.PrintWriter;
import javax.swing.*;

/** Alternative concept: layout computation time and number of container objects of the three designs. */
public class LayoutBenchmark {
    static int containers(Component c) {
        int n = 0;
        if (c instanceof Container) { n = 1; for (Component k : ((Container) c).getComponents()) n += containers(k); }
        return n;
    }
    static int panels(Component c) {            // only the helper panels we created (JPanel), not buttons / scroll panes
        int n = (c instanceof JPanel) ? 1 : 0;
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) n += panels(k);
        return n;
    }
    static void layoutAll(Container c) { c.doLayout(); for (Component k : c.getComponents()) if (k instanceof Container) layoutAll((Container) k); }

    public static void main(String[] args) throws Exception {
        String[] names = {"nested", "gridbag", "absolute"};
        int resizes = 20000;
        PrintWriter csv = new PrintWriter("layout_results.csv");
        csv.println("design,helper_panels,all_containers,components_total,microseconds_per_resize");
        System.out.println("=== Time for one resize (layout of the whole tree) and number of container objects ===");
        System.out.printf("%-10s %14s %16s %14s %22s%n", "design", "helper JPanels", "all containers", "components", "layout time (us/resize)");
        for (String n : names) {
            LibraryScreen s = n.equals("nested") ? new LibraryNested() : n.equals("gridbag") ? new LibraryGridBag() : new LibraryAbsolute();
            for (int w = 0; w < 3000; w++) { s.setSize(500 + (w % 400), 300 + (w % 200)); layoutAll(s); }      // warm-up
            double total = 0;
            for (int r = 0; r < 5; r++) {
                long t = System.nanoTime();
                for (int i = 0; i < resizes; i++) { s.setSize(500 + (i % 400), 300 + (i % 200)); layoutAll(s); }
                total += (System.nanoTime() - t) / (double) resizes / 1000.0;
            }
            double us = total / 5;
            int hp = panels(s) - 1;                                 // minus the screen itself
            int all = containers(s);
            int comps = countAll(s);
            System.out.printf("%-10s %14d %16d %14d %22.2f%n", n, hp, all, comps, us);
            csv.printf("%s,%d,%d,%d,%.2f%n", n, hp, all, comps, us);
        }
        csv.close();
    }
    static int countAll(Component c) { int n = 1; if (c instanceof Container) for (Component k : ((Container) c).getComponents()) n += countAll(k); return n; }
}
