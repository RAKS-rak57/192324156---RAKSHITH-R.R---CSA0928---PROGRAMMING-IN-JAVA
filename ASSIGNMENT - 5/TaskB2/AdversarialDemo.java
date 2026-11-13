import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Task B2: deliberately constructed adversarial inputs against the unsafe and the safe login. */
public class AdversarialDemo {
    static long sink = 0;
    static void line(String title, LoginCheck.Result r) { System.out.printf("   %-28s accepted=%-5s ids=%-26s%n", title, r.accepted(), r.ids); }

    public static void main(String[] args) throws Exception {
        minisqlite.MiniSqlite.install();
        File db = File.createTempFile("b2demo", ".db"); db.delete();
        try (Connection c = DriverManager.getConnection("jdbc:minisqlite:" + db.getAbsolutePath())) {
            try (Statement st = c.createStatement()) { for (String s : Files.readString(Path.of("users.sql")).split(";\\s*\\n")) if (!s.isBlank()) st.executeUpdate(s); }
            System.out.println("Test database: users = admin, arun, divya, obrien (passwords are test values only)");

            System.out.println();
            System.out.println("=== 1. Normal behaviour (both versions must agree) ===");
            System.out.println(" arun / secret123 :"); line("unsafe", LoginCheck.unsafe(c, "arun", "secret123")); line("PreparedStatement", LoginCheck.safe(c, "arun", "secret123"));
            System.out.println(" arun / wrongpass :"); line("unsafe", LoginCheck.unsafe(c, "arun", "wrongpass")); line("PreparedStatement", LoginCheck.safe(c, "arun", "wrongpass"));

            System.out.println();
            System.out.println("=== 2. Adversarial inputs (NO valid password is known in any of them) ===");
            String[][] attacks = {
                {"A1  password = ' OR '1'='1", "x", "' OR '1'='1"},
                {"A2  id = arun' OR '1'='1", "arun' OR '1'='1", "anything"},
                {"A3  id = admin' --  (comment)", "admin' -- ", "anything"},
            };
            for (String[] a : attacks) {
                System.out.println(" " + a[0] + "     [id=\"" + a[1] + "\", pwd=\"" + a[2] + "\"]");
                LoginCheck.Result u = LoginCheck.unsafe(c, a[1], a[2]);
                System.out.println("   SQL built by the unsafe code: " + u.sql);
                line("unsafe", u);
                line("PreparedStatement", LoginCheck.safe(c, a[1], a[2]));
            }

            System.out.println();
            System.out.println("=== 3. The weaker fixes: escaping quotes and regular-expression validation ===");
            System.out.println(" Attacks A1-A3 against the two weaker fixes:");
            for (String[] a : attacks) { line("escaped  " + a[0].substring(0, 2), LoginCheck.escaped(c, a[1], a[2])); line("regex    " + a[0].substring(0, 2), LoginCheck.regex(c, a[1], a[2])); }
            System.out.println(" Numeric context (no quotes in the SQL, user_no = <input>), input \"0 OR 1=1\":");
            line("escaped quotes", LoginCheck.findByNumber(c, "0 OR 1=1", true));
            System.out.println("   SQL: " + LoginCheck.findByNumber(c, "0 OR 1=1", true).sql);
            System.out.println(" A legitimate user whose password contains an apostrophe (obrien / it's-me):");
            line("unsafe (no protection)", safeCatch(c, "obrien", "it's-me"));
            line("escaped quotes", LoginCheck.escaped(c, "obrien", "it's-me"));
            line("regex validation", LoginCheck.regex(c, "obrien", "it's-me"));
            line("PreparedStatement", LoginCheck.safe(c, "obrien", "it's-me"));
            System.out.println(" Admin with a normal password containing # and !   (admin / Adm!n#2026):");
            line("regex validation", LoginCheck.regex(c, "admin", "Adm!n#2026"));
            line("PreparedStatement", LoginCheck.safe(c, "admin", "Adm!n#2026"));

            System.out.println();
            System.out.println("=== 4. Cost per login check (20,000 logins, microseconds per login, best of 5) ===");
            int n = 20000;
            String[] names = {"string-built Statement (unsafe)", "escape quotes + Statement", "regex validate + Statement", "PreparedStatement, prepared every time", "PreparedStatement, prepared ONCE"};
            for (int w = 0; w < 3; w++) for (int k = 0; k < 5; k++) run(c, k, 500);
            for (int k = 0; k < 5; k++) {
                double best = 1e18;
                for (int r = 0; r < 5; r++) { long t = System.nanoTime(); run(c, k, n); best = Math.min(best, (System.nanoTime() - t) / 1e3 / n); }
                System.out.printf("   %-42s %8.2f us%n", names[k], best);
            }
            long t = System.nanoTime();
            for (int i = 0; i < 2_000_000; i++) { sink += LoginCheck.SAFE_TEXT.matcher("arun" + (i & 7)).matches() ? 1 : 0; }
            double regexNs = (System.nanoTime() - t) / 2e6;
            t = System.nanoTime();
            for (int i = 0; i < 2_000_000; i++) { sink += LoginCheck.escapeQuotes("secret12" + (i & 7)).length(); }
            double escNs = (System.nanoTime() - t) / 2e6;
            System.out.printf("   validation work alone: regex match %.0f ns, quote escaping %.0f ns%n", regexNs, escNs);
            System.out.println("(checksum " + sink + ")");
        }
        db.delete();
    }

    static LoginCheck.Result safeCatch(Connection c, String id, String pwd) {
        try { return LoginCheck.unsafe(c, id, pwd); }
        catch (SQLException e) { LoginCheck.Result r = new LoginCheck.Result(); r.sql = "(SQL error: " + e.getMessage() + ")"; System.out.println("   unsafe code raised: " + e.getMessage()); return r; }
    }

    static PreparedStatement reused;
    static void run(Connection c, int kind, int n) throws SQLException {
        if (kind == 4 && reused == null) reused = c.prepareStatement("SELECT * FROM users WHERE id = ? AND pwd = ?");
        for (int i = 0; i < n; i++) {
            String id = (i % 2 == 0) ? "arun" : "divya", pwd = (i % 2 == 0) ? "secret123" : "pass456";
            switch (kind) {
                case 0: sink += LoginCheck.unsafe(c, id, pwd).ids.size(); break;
                case 1: sink += LoginCheck.escaped(c, id, pwd).ids.size(); break;
                case 2: sink += LoginCheck.regex(c, id, pwd).ids.size(); break;
                case 3: sink += LoginCheck.safe(c, id, pwd).ids.size(); break;
                default: reused.setString(1, id); reused.setString(2, pwd); try (ResultSet rs = reused.executeQuery()) { while (rs.next()) sink++; }
            }
        }
    }
}
