import java.util.ArrayList;
import java.util.List;

public class PassData {
    /** n generated passes: {passId, regNo, name, route, amount}. */
    public static List<String[]> generate(int n) {
        List<String[]> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            l.add(new String[]{"P" + (100000 + i), String.format("19%07d", i), "Student Name " + (i % 1000), String.valueOf(1 + i % 40), String.valueOf(3000 + (i % 2000) + 0.5)});
        return l;
    }
}
