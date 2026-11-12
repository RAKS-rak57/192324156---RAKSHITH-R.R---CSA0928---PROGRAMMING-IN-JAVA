import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// Experiment (Task 4 Complete the Code): @Override PUT BACK but the method is still misspelled as mouseclicked.
public class DrawPadWithOverride extends Frame {
    private final List<Point> points = new ArrayList<>();
    private final Label status = new Label("Clicks: 0");

    public DrawPadWithOverride() {
        super("Draw Pad");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseclicked(MouseEvent e) {          // the compiler now checks the name
                points.add(e.getPoint());
                status.setText("Clicks: " + points.size());
                repaint();
            }
        });
        setSize(400, 300);
    }

    @Override
    public void paint(Graphics g) {
        for (Point p : points) g.fillOval(p.x - 3, p.y - 3, 6, 6);
    }

    public static void main(String[] args) { new DrawPadWithOverride().setVisible(true); }
}
