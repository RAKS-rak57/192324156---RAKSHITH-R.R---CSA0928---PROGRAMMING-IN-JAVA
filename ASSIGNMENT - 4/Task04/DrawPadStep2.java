import java.awt.*;
import java.awt.event.*;

/**
 * Defect 1 repaired (mouseClicked, label updated) but the dots are still drawn straight onto the screen with getGraphics().
 * This is the version that loses its dots when the window is minimised and restored (Task 4.2).
 */
public class DrawPadStep2 extends Frame {
    int count = 0;
    Label status = new Label("Clicks: 0");

    public DrawPadStep2() {
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                count++;
                status.setText("Clicks: " + count);
                Graphics g = getGraphics();                      // a temporary graphics context for the window surface
                g.fillOval(e.getX(), e.getY(), 6, 6);            // DEFECT 2: nothing remembers this dot
                g.dispose();
            }
        });
        setSize(400, 300);
        setVisible(true);
    }
}
