import java.io.BufferedReader;
import java.io.FileReader;

/** Reads input/tests.txt:  label | request | card file | expected path   and prints a result table. */
public class AtmTest {
    public static void main(String[] args) throws Exception {
        int n = 0, pass = 0;
        try (BufferedReader in = new BufferedReader(new FileReader("input/tests.txt"))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] f = line.split("\\|");
                String label = f[0].trim(), request = f[1].trim(), card = f[2].trim(), expected = f[3].trim();
                n++;
                System.out.println("Test " + n + ": " + label + "   request=\"" + request + "\" card=" + card);
                int before = Atm.finallyRuns;
                String actual = Atm.withdraw(request, card);
                boolean fin = Atm.finallyRuns == before + 1;
                boolean ok = actual.equals(expected) && fin;
                if (ok) pass++;
                System.out.println("   -> path=" + actual + " expected=" + expected + " finally executed=" + fin + "  " + (ok ? "PASS" : "FAIL"));
            }
        }
        System.out.println();
        System.out.println("Result: " + pass + " / " + n + " tests passed; finally blocks executed = " + Atm.finallyRuns);
    }
}
