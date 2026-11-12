import java.util.ArrayList;
import java.util.List;

/** A very small in-memory catalogue so the search screen has something to show. */
public class BookCatalog {
    private static final String[][] BOOKS = {
        {"Programming in Java", "Balagurusamy", "Computer Science", "9780070141691"},
        {"Head First Java", "Kathy Sierra", "Computer Science", "9780596009205"},
        {"Effective Java", "Joshua Bloch", "Computer Science", "9780134685991"},
        {"Database System Concepts", "Silberschatz", "Computer Science", "9780078022159"},
        {"Operating System Concepts", "Silberschatz", "Computer Science", "9781119800361"},
        {"Engineering Mathematics", "B. S. Grewal", "Mathematics", "9788193328491"},
        {"Digital Electronics", "Morris Mano", "Electronics", "9780132774208"},
        {"Wings of Fire", "A. P. J. Abdul Kalam", "Biography", "9788173711466"},
        {"The Discovery of India", "Jawaharlal Nehru", "History", "9780143031031"},
        {"Data Structures Using C", "Reema Thareja", "Computer Science", "9780198099307"},
        {"Discrete Mathematics", "Kenneth Rosen", "Mathematics", "9780073383095"},
        {"Clean Code", "Robert C. Martin", "Computer Science", "9780132350884"}};

    public static List<String> search(String category, String text) {
        String q = text == null ? "" : text.trim().toLowerCase();
        List<String> out = new ArrayList<>();
        for (String[] b : BOOKS) {
            String field;
            switch (category) {
                case "Title":   field = b[0]; break;
                case "Author":  field = b[1]; break;
                case "Subject": field = b[2]; break;
                case "ISBN":    field = b[3]; break;
                default:        field = String.join(" ", b);
            }
            if (q.isEmpty() || field.toLowerCase().contains(q)) out.add(b[0] + "  -  " + b[1] + "  (" + b[2] + ")");
        }
        return out;
    }
}
