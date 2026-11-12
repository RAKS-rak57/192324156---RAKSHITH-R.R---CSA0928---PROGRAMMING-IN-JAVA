import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.*;
import javax.swing.*;

/** Headless demonstration of Task 5: runs every converter version through real button clicks and Enter key events. */
public class ConverterHarness {
    @SuppressWarnings("unchecked")
    static <T> T field(Object o, String name) throws Exception { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return (T) f.get(o); }

    static String run(Object win, String text, int currencyIndex, boolean useEnter) throws Exception {
        JTextField amount = field(win, "amount");
        JComboBox<?> target = field(win, "target");
        JLabel result = field(win, "result");
        JButton button = field(win, "convertButton");
        amount.setText(text); target.setSelectedIndex(currencyIndex); result.setText(" ");
        JOptionPane.lastMessage = null;
        if (useEnter) amount.postActionEvent(); else button.doClick();
        return result.getText();
    }

    static JButton buttonOf(Object win) throws Exception {
        try { return field(win, "convertButton"); }
        catch (NoSuchFieldException e) {                                  // Converter.java keeps the button in a local variable
            return findButton((Container) ((JFrame) win).getContentPane());
        }
    }
    static JButton findButton(Container c) {               // direct children only (a JComboBox contains its own arrow button)
        for (Component k : c.getComponents()) if (k instanceof JButton) return (JButton) k;
        return null;
    }

    public static void main(String[] args) throws Exception {
        String[] names = {"ConverterA", "ConverterB", "ConverterC", "ConverterD", "ConverterA2", "ConverterB2", "ConverterC2", "ConverterD2", "ConverterE", "ConverterF"};
        System.out.println("=== Task 5: every version, amount 5000 INR -> EUR (index 1) and invalid text 'abc' ===");
        System.out.printf("%-12s %-12s %-14s %-14s %-14s %-22s%n", "class", "button click", "Enter key", "invalid 'abc'", "dialog shown?", "dialog text");
        for (String n : names) {
            Object win = Class.forName(n).getDeclaredConstructor().newInstance();
            ((Frame) win).setVisible(true);
            String viaButton = run(win, "5000", 1, false);
            String dialog1 = JOptionPane.lastMessage;
            String viaEnter = run(win, "5000", 1, true);
            String dialog2 = JOptionPane.lastMessage;
            String bad = run(win, "abc", 1, false);
            boolean enterWorks = viaEnter.equals("55.00 EUR");
            System.out.printf("%-12s %-12s %-14s %-14s %-14s %-22s%n", n, viaButton, enterWorks ? viaEnter : "(nothing)", bad,
                    dialog1 != null ? "yes" : "no", dialog1 != null ? dialog1 : "-");
        }

        // the "Complete the Code" version
        System.out.println();
        System.out.println("=== Complete the Code: Converter.java (lambda, one listener for button and Enter) ===");
        Converter conv = new Converter();
        conv.setVisible(true);
        JTextField amount = field(conv, "amount"); JLabel result = field(conv, "result"); JComboBox<?> target = field(conv, "target");
        JButton button = buttonOf(conv);
        amount.setText("2500"); target.setSelectedIndex(0); button.doClick();
        System.out.println("2500 INR -> USD, button click : " + result.getText());
        HeadlessRender.save(FrameRender.of(conv), "../screenshots/task05/t5_01_valid_conversion.png");
        amount.setText("5000"); target.setSelectedIndex(1); amount.postActionEvent();
        System.out.println("5000 INR -> EUR, Enter key    : " + result.getText());
        amount.setText("12abc"); button.doClick();
        System.out.println("'12abc'                       : " + result.getText());
        HeadlessRender.save(FrameRender.of(conv), "../screenshots/task05/t5_02_invalid_amount.png");
        amount.setText(""); button.doClick();
        System.out.println("empty field                   : " + result.getText());
        System.out.println("listeners on the button / on the text field : " + button.getActionListeners().length + " / " + amount.getActionListeners().length
                + "   same object? " + (button.getActionListeners()[0] == amount.getActionListeners()[0]));

        // listener objects per version (space) and number of triggers
        System.out.println();
        System.out.println("=== Listener objects registered per version (distinct objects) ===");
        for (String n : new String[]{"ConverterA2", "ConverterB2", "ConverterC2", "ConverterD2", "ConverterE", "ConverterF"}) {
            Object win = Class.forName(n).getDeclaredConstructor().newInstance();
            JTextField a = field(win, "amount"); JButton b = field(win, "convertButton");
            Set<Object> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
            int registrations = 0;
            for (ActionListener l : b.getActionListeners()) { distinct.add(l); registrations++; }
            for (ActionListener l : a.getActionListeners()) { distinct.add(l); registrations++; }
            if (n.equals("ConverterF")) {
                JMenuItem mi = field(win, "convertMenuItem");
                for (ActionListener l : mi.getActionListeners()) { distinct.add(l); registrations++; }
            }
            System.out.printf("%-12s registrations: %d   distinct listener objects: %d%n", n, registrations, distinct.size());
        }
        // ConverterF shared Action
        Object f = Class.forName("ConverterF").getDeclaredConstructor().newInstance();
        JButton fb = field(f, "convertButton"); JMenuItem fm = field(f, "convertMenuItem"); JTextField fa = field(f, "amount"); JLabel fr = field(f, "result");
        fa.setText("100"); fm.doClick();
        System.out.println("ConverterF menu item click (same Action) -> result label: " + fr.getText() + "; Action shared by button and menu item: " + (fb.getAction() == fm.getAction()));
    }
}
