package javax.swing;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
/** TEST-ONLY stand-in for JOptionPane: no dialog is shown, the call is recorded and a scripted answer is returned. */
public class JOptionPane {
    public static final int ERROR_MESSAGE = 0, INFORMATION_MESSAGE = 1, WARNING_MESSAGE = 2, QUESTION_MESSAGE = 3, PLAIN_MESSAGE = -1;
    public static final int DEFAULT_OPTION = -1, YES_NO_OPTION = 0, YES_NO_CANCEL_OPTION = 1, OK_CANCEL_OPTION = 2;
    public static final int YES_OPTION = 0, NO_OPTION = 1, CANCEL_OPTION = 2, OK_OPTION = 0, CLOSED_OPTION = -1;
    public static final List<String> log = new ArrayList<>();
    public static int nextConfirm = YES_OPTION;
    public static String lastMessage = null, lastTitle = null;
    public static void showMessageDialog(Component p, Object m) { showMessageDialog(p, m, "Message", INFORMATION_MESSAGE); }
    public static void showMessageDialog(Component p, Object m, String title, int type) { lastMessage = String.valueOf(m); lastTitle = title; log.add("[dialog:" + title + "] " + m); }
    public static int showConfirmDialog(Component p, Object m) { return showConfirmDialog(p, m, "Confirm", YES_NO_OPTION); }
    public static int showConfirmDialog(Component p, Object m, String title, int option) { lastMessage = String.valueOf(m); lastTitle = title; log.add("[confirm:" + title + "] " + m + " -> " + nextConfirm); return nextConfirm; }
}
