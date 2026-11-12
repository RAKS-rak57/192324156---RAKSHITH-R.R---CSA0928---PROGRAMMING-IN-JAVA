import java.awt.*;
import java.awt.event.*;

/**
 * Run this on a machine WITH a display to print the real order of the window events (Task 2.4):
 *   javac WindowTrace.java  &&  java WindowTrace
 * Then minimise, restore and close the window and paste the console output into the report.
 */
public class WindowTrace extends Frame implements WindowListener, WindowFocusListener, WindowStateListener {
    public WindowTrace() {
        super("Window trace");
        System.out.println("constructor finished");
        addWindowListener(this);
        addWindowFocusListener(this);
        addWindowStateListener(this);
        setSize(300, 150);
    }
    public void windowOpened(WindowEvent e)      { System.out.println("windowOpened"); }
    public void windowClosing(WindowEvent e)     { System.out.println("windowClosing"); dispose(); }
    public void windowClosed(WindowEvent e)      { System.out.println("windowClosed"); System.exit(0); }
    public void windowIconified(WindowEvent e)   { System.out.println("windowIconified"); }
    public void windowDeiconified(WindowEvent e) { System.out.println("windowDeiconified"); }
    public void windowActivated(WindowEvent e)   { System.out.println("windowActivated"); }
    public void windowDeactivated(WindowEvent e) { System.out.println("windowDeactivated"); }
    public void windowGainedFocus(WindowEvent e) { System.out.println("windowGainedFocus"); }
    public void windowLostFocus(WindowEvent e)   { System.out.println("windowLostFocus"); }
    public void windowStateChanged(WindowEvent e){ System.out.println("windowStateChanged"); }
    public void paint(Graphics g)                { System.out.println("paint"); }
    public static void main(String[] args) { System.out.println("main: setVisible(true)"); new WindowTrace().setVisible(true); }
}
