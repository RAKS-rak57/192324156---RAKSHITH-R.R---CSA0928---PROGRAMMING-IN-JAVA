import java.io.*;
public class UnreportedChecked {
    public static void main(String[] a) {
        new FileReader("cards.txt");      // checked FileNotFoundException neither caught nor declared
    }
}
