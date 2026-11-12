package java.awt;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
/** TEST-ONLY stand-in for java.awt.Frame so GUI programs can run in a headless JVM (no display, no native peers). */
public class Frame extends Panel {
    public static final int NORMAL = 0, ICONIFIED = 1;
    private String title = "";
    private final List<WindowListener> wl = new ArrayList<>();
    public Frame() { this(""); }
    public Frame(String title) { super(new BorderLayout()); this.title = title; }
    public Frame(String title, GraphicsConfiguration gc) { this(title); }
    public String getTitle() { return title; }
    public void setTitle(String t) { title = t; }
    public synchronized void addWindowListener(WindowListener l) { wl.add(l); }
    public void addWindowFocusListener(WindowFocusListener l) { }
    public void addWindowStateListener(WindowStateListener l) { }
    public void setExtendedState(int s) { }
    public void setIconImage(Image i) { }
    public void setResizable(boolean b) { }
    public void setLocationRelativeTo(Component c) { }
    public void setDefaultLookAndFeelDecorated(boolean b) { }
    public void pack() { setSize(getPreferredSize()); layoutAll(this); }
    public void dispose() { }
    public static void layoutAll(Container c) { c.doLayout(); for (Component k : c.getComponents()) if (k instanceof Container) layoutAll((Container) k); }
    /** Paint the whole component tree into an image (what the screen would show). */
    public BufferedImage snapshot() {
        layoutAll(this);
        BufferedImage im = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.BLACK);                     // a real window sets its foreground colour before paint()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paint(g);                                   // the frame's own paint() (user code may draw here)
        for (int i = getComponentCount() - 1; i >= 0; i--) {   // then the children, as native peers would draw themselves
            Component k = getComponent(i);
            if (!k.isVisible()) continue;
            Graphics cg = g.create(k.getX(), k.getY(), k.getWidth(), k.getHeight());
            k.paint(cg); cg.dispose();
        }
        g.dispose(); return im;
    }
}
