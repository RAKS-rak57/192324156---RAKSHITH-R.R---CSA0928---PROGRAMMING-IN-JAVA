import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Lays out each design at three window sizes, prints the region sizes and saves a picture. Not part of the application. */
public class ResizeHarness {
    static String dim(Component c) { return c.getWidth() + "x" + c.getHeight(); }

    public static void main(String[] args) throws Exception {
        int[][] sizes = {{520, 330}, {800, 500}, {1100, 650}};
        String[] names = {"nested", "gridbag", "absolute"};
        System.out.println("Region sizes (width x height in pixels) after layout at three window sizes");
        for (String n : names) {
            System.out.println();
            System.out.println("DESIGN: " + n);
            System.out.printf("%-12s %-10s %-10s %-10s %-10s %-10s %-10s%n", "window", "results", "search fld", "Search btn", "Prev btn", "status", "title");
            int firstResultsH = 0, firstResultsW = 0, firstBtnW = 0, firstBtnH = 0;
            for (int i = 0; i < sizes.length; i++) {
                final int w = sizes[i][0], h = sizes[i][1];
                final LibraryScreen[] s = new LibraryScreen[1];
                SwingUtilities.invokeAndWait(() -> s[0] = n.equals("nested") ? new LibraryNested() : n.equals("gridbag") ? new LibraryGridBag() : new LibraryAbsolute());
                LibraryScreen screen = s[0];
                screen.setSize(w, h);
                HeadlessRender.layoutAll(screen);
                System.out.printf("%-12s %-10s %-10s %-10s %-10s %-10s %-10s%n", w + "x" + h, dim(screen.resultsPane()), dim(screen.queryField()),
                        dim(screen.searchButton()), dim(screen.prevButton()), dim(screen.statusLabel()), dim(screen.titleLabel()));
                if (i == 0) { firstResultsW = screen.resultsPane().getWidth(); firstResultsH = screen.resultsPane().getHeight(); firstBtnW = screen.searchButton().getWidth(); firstBtnH = screen.searchButton().getHeight(); }
                else {
                    boolean grew = screen.resultsPane().getWidth() > firstResultsW && screen.resultsPane().getHeight() > firstResultsH;
                    boolean btnSame = screen.searchButton().getWidth() == firstBtnW && screen.searchButton().getHeight() == firstBtnH;
                    System.out.println("             results grew: " + grew + "   Search button kept its size: " + btnSame);
                }
                screen.searchButton().doClick();                       // fill the results area so the picture shows data
                HeadlessRender.save(HeadlessRender.render(screen, w, h, "Campus Library Search - " + n + " - " + w + "x" + h),
                        "../screenshots/task03/t3_" + n + "_" + w + "x" + h + ".png");
            }
        }
    }
}
