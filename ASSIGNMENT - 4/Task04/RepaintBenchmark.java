import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Alternative concept: time per repaint and memory for n clicks - redraw n points  versus  copy one off-screen image. */
public class RepaintBenchmark {
    static long sink = 0;
    static long allocated() {
        com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        return b.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    public static void main(String[] args) throws Exception {
        int[] ns = {100, 1000, 10000, 100000, 1000000};
        int w = 400, h = 300;
        BufferedImage screen = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);       // stands for the window surface
        Graphics2D sg = screen.createGraphics();
        Random rnd = new Random(1);
        PrintWriter csv = new PrintWriter("repaint_results.csv");
        csv.println("n,points_repaint_us,image_repaint_us,points_memory_bytes,image_memory_bytes");
        System.out.println("=== Time of ONE repaint (microseconds) and memory, window 400 x 300 ===");
        System.out.printf("%10s %20s %20s %20s %20s%n", "n clicks", "redraw n points", "copy 1 image", "points memory (B)", "image memory (B)");

        for (int n : ns) {
            long a0 = allocated();
            List<Point> points = new ArrayList<>();
            for (int i = 0; i < n; i++) points.add(new Point(rnd.nextInt(w), rnd.nextInt(h)));
            long pointBytes = allocated() - a0;

            BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = canvas.createGraphics();
            for (Point p : points) cg.fillOval(p.x - 3, p.y - 3, 6, 6);                      // the dots are drawn once
            cg.dispose();
            long imageBytes = (long) w * h * 4;

            int reps = n >= 100000 ? 5 : 50;
            for (int i = 0; i < 3; i++) { redraw(sg, points); sg.drawImage(canvas, 0, 0, null); }   // warm-up
            long t = System.nanoTime();
            for (int i = 0; i < reps; i++) redraw(sg, points);
            double pointsUs = (System.nanoTime() - t) / (double) reps / 1000.0;
            int reps2 = 2000;
            t = System.nanoTime();
            for (int i = 0; i < reps2; i++) sg.drawImage(canvas, 0, 0, null);
            double imageUs = (System.nanoTime() - t) / (double) reps2 / 1000.0;

            System.out.printf("%10d %20.1f %20.1f %20d %20d%n", n, pointsUs, imageUs, pointBytes, imageBytes);
            csv.printf("%d,%.1f,%.1f,%d,%d%n", n, pointsUs, imageUs, pointBytes, imageBytes);
        }
        csv.close();
        System.out.println("Image memory for a full-HD window (1920 x 1080 x 4 bytes) = " + (1920L * 1080 * 4) + " bytes");
        System.out.println("(checksum " + sink + ")");
    }

    static void redraw(Graphics2D g, List<Point> points) {
        g.setColor(Color.WHITE); g.fillRect(0, 0, 400, 300); g.setColor(Color.BLACK);
        for (Point p : points) g.fillOval(p.x - 3, p.y - 3, 6, 6);
        sink += points.size();
    }
}
