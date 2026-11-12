import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class DrawPad extends Frame {
    private final List<Point> points = new ArrayList<>();
    private final Label status = new Label("Clicks: 0");

    public DrawPad() {
        super("Draw Pad");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            // TODO 1 (completed): override mouseClicked correctly: store the point, update the label and request a repaint
            @Override
            public void mouseClicked(MouseEvent e) {
                points.add(e.getPoint());
                status.setText("Clicks: " + points.size());
                repaint();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        setSize(400, 300);
    }

    @Override
    public void paint(Graphics g) {
        // TODO 2 (completed): draw every stored point as a 6 x 6 filled circle centred on the point
        super.paint(g);
        for (Point p : points) {
            g.fillOval(p.x - 3, p.y - 3, 6, 6);
        }
    }

    public int clickCount() { return points.size(); }          // small accessor, used only by the tests

    public static void main(String[] args) {
        new DrawPad().setVisible(true);
    }
}
