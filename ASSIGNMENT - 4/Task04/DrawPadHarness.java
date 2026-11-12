import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Headless demonstration of Task 4 (not part of the application). The window events are real MouseEvents sent to the real classes.
 * "Minimise and restore" is imitated by clearing what is on the screen and asking the frame to paint itself again.
 */
public class DrawPadHarness {
    static final int[][] CLICKS = {{60, 70}, {150, 120}, {240, 90}, {320, 200}, {100, 220}};
    static final String DIR = "../screenshots/task04/";

    static void click(Frame f, int x, int y) {
        f.dispatchEvent(new MouseEvent(f, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false));
    }
    static BufferedImage overlay(BufferedImage base, BufferedImage dots) {
        BufferedImage out = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics(); g.drawImage(base, 0, 0, null); g.drawImage(dots, 0, 0, null); g.dispose(); return out;
    }

    public static void main(String[] args) throws Exception {
        // ---------- 1. the original program ----------
        DrawPadBuggy buggy = new DrawPadBuggy();
        for (int[] c : CLICKS) click(buggy, c[0], c[1]);
        System.out.println("=== 1. Original DrawPadBuggy: 5 mouse clicks sent to the window ===");
        System.out.println("count = " + buggy.count + "      label = \"" + buggy.status.getText() + "\"");
        Object listener = buggy.getMouseListeners()[0];
        System.out.println("methods declared by the listener class : " + Arrays.toString(Arrays.stream(listener.getClass().getDeclaredMethods()).map(m -> m.getName()).toArray()));
        boolean overrides = true;
        try { listener.getClass().getDeclaredMethod("mouseClicked", MouseEvent.class); } catch (NoSuchMethodException e) { overrides = false; }
        System.out.println("listener overrides mouseClicked(MouseEvent) : " + overrides + "  -> the inherited empty method of MouseAdapter runs");
        HeadlessRender.save(FrameRender.of(buggy), DIR + "t4_01_original_after_5_clicks.png");

        // ---------- 2. spelling fixed, but dots drawn with getGraphics() ----------
        final BufferedImage dotLayer = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);   // what getGraphics() paints on
        DrawPadStep2 step2 = new DrawPadStep2() {
            @Override public Graphics getGraphics() { Graphics2D g = dotLayer.createGraphics(); g.setColor(Color.BLACK); return g; }
        };
        for (int[] c : CLICKS) click(step2, c[0], c[1]);
        System.out.println();
        System.out.println("=== 2. Spelling fixed, dots drawn with getGraphics() ===");
        System.out.println("after 5 clicks              : count = " + step2.count + ", label = \"" + step2.status.getText() + "\", dots on screen = 5");
        HeadlessRender.save(HeadlessRender.withTitle(overlay(step2.snapshot(), dotLayer), "Draw Pad"), DIR + "t4_02_step2_before_minimise.png");
        Graphics2D clear = dotLayer.createGraphics(); clear.setComposite(AlphaComposite.Clear); clear.fillRect(0, 0, 400, 300); clear.dispose();   // the window system throws the pixels away
        System.out.println("after minimise + restore    : paint() runs again; Frame.paint knows nothing about the dots -> dots on screen = 0, label still \"" + step2.status.getText() + "\"");
        HeadlessRender.save(HeadlessRender.withTitle(overlay(step2.snapshot(), dotLayer), "Draw Pad"), DIR + "t4_03_step2_after_restore.png");

        // ---------- 3. completed DrawPad (Complete the Code) ----------
        DrawPad pad = new DrawPad();
        pad.setVisible(true);
        for (int[] c : CLICKS) click(pad, c[0], c[1]);
        System.out.println();
        System.out.println("=== 3. Completed DrawPad (TODO 1 and TODO 2) ===");
        System.out.println("after 5 clicks              : clickCount() = " + pad.clickCount() + ", dots painted from the stored points");
        HeadlessRender.save(FrameRender.of(pad), DIR + "t4_04_drawpad_before_minimise.png");
        System.out.println("after minimise + restore    : paint() draws the stored points again -> still 5 dots, same picture");
        HeadlessRender.save(FrameRender.of(pad), DIR + "t4_05_drawpad_after_restore.png");
        click(pad, 20, 20);
        System.out.println("one more click              : clickCount() = " + pad.clickCount());

        // ---------- 4. BufferedImage version ----------
        DrawPadBuffered buf = new DrawPadBuffered();
        buf.setVisible(true);
        for (int[] c : CLICKS) click(buf, c[0], c[1]);
        System.out.println();
        System.out.println("=== 4. DrawPadBuffered (off-screen image) ===");
        System.out.println("after 5 clicks              : clickCount() = " + buf.clickCount());
        HeadlessRender.save(FrameRender.of(buf), DIR + "t4_06_buffered_after_restore.png");
    }
}
