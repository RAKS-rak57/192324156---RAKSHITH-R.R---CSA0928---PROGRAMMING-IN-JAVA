public class MultiCatchRelated {
    public static void main(String[] a) {
        try { Integer.parseInt("x"); }
        catch (NumberFormatException | IllegalArgumentException e) { }   // NumberFormatException IS-A IllegalArgumentException
    }
}
