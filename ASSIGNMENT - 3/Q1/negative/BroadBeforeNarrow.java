import java.io.*;
public class BroadBeforeNarrow {
    public static void main(String[] a) {
        try { new FileReader("x.txt").close(); }
        catch (Exception e) { System.out.println("any exception"); }      // broad handler first
        catch (FileNotFoundException e) { System.out.println("file missing"); }  // can never be reached
    }
}
