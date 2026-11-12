import java.awt.*;
import java.awt.event.*;

/** The ORIGINAL program from the assignment (renamed DrawPadBuggy so that it can live beside the repaired DrawPad). */
public class DrawPadBuggy extends Frame {
    int count = 0;
    Label status = new Label("Clicks: 0");

    public DrawPadBuggy() {
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            public void mouseclicked(MouseEvent e) {            // DEFECT 1: wrong spelling (small c)
                count++;
                Graphics g = getGraphics();
                g.fillOval(e.getX(), e.getY(), 6, 6);
            }
        });
        setSize(400, 300);
        setVisible(true);
    }
}
