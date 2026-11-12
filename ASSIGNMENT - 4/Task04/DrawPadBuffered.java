import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/** Alternative concept: each dot is drawn ONCE onto an off-screen image; paint() only copies that image to the window. */
public class DrawPadBuffered extends Frame {
    private BufferedImage canvas = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
    private int clicks = 0;
    private final Label status = new Label("Clicks: 0");

    public DrawPadBuffered() {
        super("Draw Pad (BufferedImage)");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Graphics2D g = canvas.createGraphics();
                g.setColor(Color.BLACK);
                g.fillOval(e.getX() - 3, e.getY() - 3, 6, 6);     // drawn once, never again
                g.dispose();
                clicks++;
                status.setText("Clicks: " + clicks);
                repaint();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        setSize(400, 300);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(canvas, 0, 0, null);                          // one image copy, no matter how many clicks
    }

    public int clickCount() { return clicks; }
    public static void main(String[] args) { new DrawPadBuffered().setVisible(true); }
}
