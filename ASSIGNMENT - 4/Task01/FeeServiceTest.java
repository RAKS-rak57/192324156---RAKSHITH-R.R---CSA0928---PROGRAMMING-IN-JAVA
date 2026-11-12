/** Plain-Java tests of the service (no window needed). Run: java FeeServiceTest */
public class FeeServiceTest {
    static int passed = 0, failed = 0;

    static void check(String name, PaymentResult r, boolean expectSuccess, String expectedPart) {
        boolean ok = r.success == expectSuccess && r.message.contains(expectedPart);
        if (ok) passed++; else failed++;
        System.out.printf("%-4s %-38s success=%-5s message=%s%n", ok ? "PASS" : "FAIL", name, r.success, r.message);
    }

    public static void main(String[] args) {
        InMemoryPaymentRepository repo = new InMemoryPaymentRepository();
        FeeService service = new FeeService(repo, new SimulatedBank(0));

        System.out.println("=== FeeService tests ===");
        check("valid payment",                 service.pay("192311001", "15000"),  true,  "received");
        check("valid payment with spaces",     service.pay("  192311002 ", " 2500.50 "), true, "received");
        check("empty register number",         service.pay("", "100"),             false, "required");
        check("short register number",         service.pay("1923", "100"),         false, "9 digits");
        check("empty amount",                  service.pay("192311001", ""),       false, "Amount is required");
        check("amount is text",                service.pay("192311001", "abc"),    false, "must be a number");
        check("negative amount",               service.pay("192311001", "-50"),    false, "between");
        check("zero amount",                   service.pay("192311001", "0"),      false, "between");
        check("amount too large",              service.pay("192311001", "999999"), false, "between");
        check("NaN amount",                    service.pay("192311001", "NaN"),    false, "between");
        check("SQL injection in reg number",   service.pay("1'); DROP TABLE fees;--", "100"), false, "9 digits");
        check("null inputs",                   service.pay(null, null),            false, "required");

        System.out.println();
        System.out.println("Payments stored after all tests : " + repo.count() + "  (expected 2 - invalid input never reaches the database)");
        System.out.println("Receipt text for 192311001:");
        System.out.println(service.receipt("192311001"));
        System.out.println("Receipt text for 192311009 (no payments):");
        System.out.println(service.receipt("192311009"));
        System.out.println();
        System.out.println("Result: " + passed + " passed, " + failed + " failed");

        // why the original  e.getActionCommand() == "Pay"  is unreliable
        String fromResource = new String("Pay");
        System.out.println();
        System.out.println("\"Pay\" == \"Pay\" (same literal)          : " + ("Pay" == "Pay"));
        System.out.println("new String(\"Pay\") == \"Pay\"             : " + (fromResource == "Pay") + "   <- == compares references");
        System.out.println("new String(\"Pay\").equals(\"Pay\")        : " + fromResource.equals("Pay"));

        // how the original built its SQL
        String evil = "x', 0); DROP TABLE fees; --";
        System.out.println();
        System.out.println("Original code builds : INSERT INTO fees VALUES('" + evil + "'," + 100.0 + ")");
        System.out.println("Repaired code sends  : INSERT INTO fees(reg_no, amount) VALUES (?, ?)   with ? = text \"" + evil + "\" as data only");
    }
}
