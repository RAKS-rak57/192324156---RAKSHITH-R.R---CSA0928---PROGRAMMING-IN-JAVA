import java.awt.Frame;
import java.awt.image.BufferedImage;
/** Works only with the headless Frame shim (uses Frame.snapshot()). */
public class FrameRender {
    public static BufferedImage of(Frame f) { return HeadlessRender.withTitle(f.snapshot(), f.getTitle()); }
}
