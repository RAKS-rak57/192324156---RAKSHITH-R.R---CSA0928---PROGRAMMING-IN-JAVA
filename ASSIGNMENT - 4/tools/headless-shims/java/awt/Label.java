package java.awt;
/** TEST-ONLY stand-in for java.awt.Label (no native peer): paints its text itself. */
public class Label extends Component {
    public static final int LEFT = 0, CENTER = 1, RIGHT = 2;
    private String text; private int alignment;
    public Label() { this(""); }
    public Label(String t) { this(t, LEFT); }
    public Label(String t, int a) { text = t == null ? "" : t; alignment = a; }
    public String getText() { return text; }
    public void setText(String t) { text = t; }
    public int getAlignment() { return alignment; }
    public void setAlignment(int a) { alignment = a; }
    public Dimension getPreferredSize() { FontMetrics fm = getFontMetrics(getFont() == null ? new Font("Dialog", Font.PLAIN, 12) : getFont()); return new Dimension(fm.stringWidth(text) + 8, fm.getHeight() + 4); }
    public Dimension getMinimumSize() { return getPreferredSize(); }
    public void paint(Graphics g) { g.setColor(Color.BLACK); g.setFont(getFont() == null ? new Font("Dialog", Font.PLAIN, 12) : getFont()); g.drawString(text, 4, g.getFontMetrics().getAscent() + 2); }
}
