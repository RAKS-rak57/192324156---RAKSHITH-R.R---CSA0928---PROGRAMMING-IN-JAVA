import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.imageio.ImageIO;
import java.io.File;

/** Test harness helper: paints a Swing component tree off-screen into a PNG (no display needed). */
public class HeadlessRender {
    public static void layoutAll(Container c) {
        c.doLayout();
        for (Component k : c.getComponents()) if (k instanceof Container) layoutAll((Container) k);
    }
    /** Lays out comp at w x h, paints it and adds a simple title bar so the picture looks like a window. */
    public static BufferedImage render(JComponent comp, int w, int h, String title) {
        comp.setSize(w, h);
        layoutAll(comp);
        return withTitle(paint(comp, w, h), title);
    }
    public static BufferedImage paint(Component comp, int w, int h) {
        BufferedImage im = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        comp.paint(g);
        g.dispose();
        return im;
    }
    public static BufferedImage withTitle(BufferedImage body, String title) {
        int bar = 26;
        BufferedImage out = new BufferedImage(body.getWidth(), body.getHeight() + bar, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xDDE3EC)); g.fillRect(0, 0, out.getWidth(), bar);
        g.setColor(new Color(0x333333)); g.setFont(new Font("Dialog", Font.BOLD, 12)); g.drawString(title, 10, 17);
        g.setColor(new Color(0xC0392B)); g.fillOval(out.getWidth() - 22, 7, 12, 12);
        g.setColor(new Color(0xF1C40F)); g.fillOval(out.getWidth() - 40, 7, 12, 12);
        g.drawImage(body, 0, bar, null);
        g.setColor(new Color(0x888888)); g.drawRect(0, 0, out.getWidth() - 1, out.getHeight() - 1);
        g.dispose();
        return out;
    }
    public static void save(BufferedImage im, String path) throws Exception {
        new File(path).getParentFile().mkdirs();
        ImageIO.write(im, "png", new File(path));
    }
    public static void edtIdle() throws Exception { javax.swing.SwingUtilities.invokeAndWait(() -> { }); }
}
