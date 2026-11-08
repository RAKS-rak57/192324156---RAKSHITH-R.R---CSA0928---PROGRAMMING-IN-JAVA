import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Q5 - Student-derived ranking design, tests with tie data and with faulty comparators,
 * and measurements for the alternative designs.
 * Uses Team and Leaderboard<T> from LeaderboardDemo.java (compile both files together).
 */
public class DesignTests {

    // ---------- Design option 1: Comparable (ONE natural order stored inside the class) ----------
    static class ComparableTeam extends Team implements Comparable<ComparableTeam> {
        ComparableTeam(String n, int p, int g) { super(n, p, g); }
        @Override
        public int compareTo(ComparableTeam o) {
            if (points != o.points) return Integer.compare(o.points, points);      // high first
            if (goalDiff != o.goalDiff) return Integer.compare(o.goalDiff, goalDiff);
            return name.compareTo(o.name);
        }
    }

    // ---------- A college team: a subclass, used to show wildcards ----------
    static class CollegeTeam extends Team {
        CollegeTeam(String n, int p, int g) { super(n, p, g); }
    }

    // ---------- Generic board with a BOUNDED type: T must be Comparable ----------
    static class NaturalBoard<T extends Comparable<? super T>> {
        private final List<T> items = new ArrayList<>();
        // wildcard: accepts a List of T or of any subclass of T
        void addAll(List<? extends T> more) { items.addAll(more); }
        List<T> ranking() {
            List<T> copy = new ArrayList<>(items);
            Collections.sort(copy);                 // uses compareTo
            return copy;
        }
    }

    // ---------- the final comparator chain ----------
    static final Comparator<Team> BY_RANK = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    // ---------- FAULTY comparators ----------
    // (a) compares points by subtraction -> int overflow for very large / very negative values
    static final Comparator<Team> FAULTY_SUBTRACT = (a, b) -> {
        if (b.points - a.points != 0) return b.points - a.points;
        if (b.goalDiff - a.goalDiff != 0) return b.goalDiff - a.goalDiff;
        return a.name.compareTo(b.name);
    };
    // (b) forgets the tie-breakers (name and goal difference are ignored)
    static final Comparator<Team> FAULTY_NO_TIEBREAK = (a, b) -> Integer.compare(b.points, a.points);

    // ---------- a checker that tests the principle on every pair / triple ----------
    static <T> int countViolations(List<T> data, Comparator<? super T> c, List<String> samples) {
        int bad = 0;
        int n = data.size();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                T a = data.get(i), b = data.get(j);
                int ab = Integer.signum(c.compare(a, b)), ba = Integer.signum(c.compare(b, a));
                if (ab != -ba) { bad++; if (samples.size() < 2) samples.add("antisymmetry: " + a + " vs " + b); }
                for (int k = 0; k < n; k++) {
                    T cc = data.get(k);
                    int bc = Integer.signum(c.compare(b, cc)), ac = Integer.signum(c.compare(a, cc));
                    if (ab < 0 && bc < 0 && ac >= 0) { bad++; if (samples.size() < 2) samples.add("transitivity: " + a + " > " + b + " > " + cc + " but not " + a + " > " + cc); }
                    if (ab == 0 && bc != ac) { bad++; if (samples.size() < 2) samples.add("equal items " + a + "," + b + " compare differently with " + cc); }
                }
            }
        }
        return bad;
    }

    static List<Team> dataSetA() {
        List<Team> t = new ArrayList<>();
        t.add(new Team("Panthers", 9, 4));
        t.add(new Team("Eagles", 9, 6));
        t.add(new Team("Cobras", 6, 1));
        t.add(new Team("Bisons", 9, 4));
        t.add(new Team("Wolves", 6, 1));
        t.add(new Team("Hawks", 6, 3));
        t.add(new Team("Lions", 3, -2));
        t.add(new Team("Tigers", 3, -2));
        t.add(new Team("Sharks", 3, 0));
        t.add(new Team("Falcons", 9, 6));
        t.add(new Team("Falcons", 9, 6));    // two teams that are completely equal
        return t;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Q5 : tests on Data Set A (ties at every level) ===");
        List<Team> a = dataSetA();
        Leaderboard<Team> board = new Leaderboard<>(BY_RANK);
        for (Team t : a) board.add(t);
        System.out.println("Ranking with comparator chain : " + board.ranking());
        System.out.println("Run again (original list unchanged, ranking() works on a copy) : " + board.ranking().equals(board.ranking()));
        List<String> samples = new ArrayList<>();
        System.out.println("Principle check on every pair/triple of Data Set A -> violations = " + countViolations(a, BY_RANK, samples));

        // ---------- Data set B: break the faulty comparators ----------
        System.out.println();
        System.out.println("=== Data Set B : designed to break faulty comparators ===");
        List<Team> b = new ArrayList<>();
        b.add(new Team("Alpha", 2_000_000_000, 0));
        b.add(new Team("Bravo", -2_000_000_000, 0));
        b.add(new Team("Charlie", 5, 0));
        b.add(new Team("Delta", 5, 0));
        b.add(new Team("Echo", 5, 3));
        b.add(new Team("Zulu", 5, 3));
        List<Team> sorted = new ArrayList<>(b);
        sorted.sort(BY_RANK);
        System.out.println("Correct chain          : " + sorted + "   violations=" + countViolations(b, BY_RANK, new ArrayList<>()));

        List<Team> s1 = new ArrayList<>(b);
        s1.sort(FAULTY_SUBTRACT);
        List<String> smp1 = new ArrayList<>();
        int v1 = countViolations(b, FAULTY_SUBTRACT, smp1);
        System.out.println("Faulty (subtraction)   : " + s1 + "   violations=" + v1);
        for (String s : smp1) System.out.println("      example -> " + s);
        System.out.println("      (Alpha has 2,000,000,000 points, Bravo has -2,000,000,000; the subtraction overflows, so Alpha is wrongly placed BELOW Bravo)");

        // faulty (b): same teams, two different input orders -> two different rankings
        List<Team> in1 = new ArrayList<>(b), in2 = new ArrayList<>(b);
        Collections.reverse(in2);
        in1.sort(FAULTY_NO_TIEBREAK);
        in2.sort(FAULTY_NO_TIEBREAK);
        System.out.println("Faulty (no tie-break)  : input order 1 -> " + in1);
        System.out.println("                         input order 2 -> " + in2);
        System.out.println("      same teams, different output order: " + !in1.toString().equals(in2.toString()) + "  (ranking depends on input order, so it is not a true ranking)");
        List<Team> c1 = new ArrayList<>(b), c2 = new ArrayList<>(b);
        Collections.reverse(c2);
        c1.sort(BY_RANK); c2.sort(BY_RANK);
        System.out.println("Correct chain, both input orders give same ranking: " + c1.toString().equals(c2.toString()));

        // faulty (a) on a big random list: TimSort may detect the broken contract
        Random rnd = new Random(5);
        List<Team> big = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            int pts = rnd.nextBoolean() ? 2_000_000_000 - rnd.nextInt(1000) : -2_000_000_000 + rnd.nextInt(1000);
            big.add(new Team("T" + i, pts, rnd.nextInt(10)));
        }
        try {
            big.sort(FAULTY_SUBTRACT);
            System.out.println("Faulty (subtraction) on 2000 random teams: no exception thrown, but the order is wrong");
        } catch (IllegalArgumentException e) {
            System.out.println("Faulty (subtraction) on 2000 random teams: IllegalArgumentException -> " + e.getMessage());
        }

        // ---------- generics, bounded types and wildcards ----------
        System.out.println();
        System.out.println("=== Generics, bounded type and wildcard demonstration ===");
        List<ComparableTeam> ct = new ArrayList<>();
        ct.add(new ComparableTeam("Panthers", 9, 4));
        ct.add(new ComparableTeam("Eagles", 9, 6));
        ct.add(new ComparableTeam("Cobras", 6, 1));
        ct.add(new ComparableTeam("Bisons", 9, 4));
        NaturalBoard<ComparableTeam> nb = new NaturalBoard<>();
        nb.addAll(ct);                                   // List<ComparableTeam>
        System.out.println("NaturalBoard<T extends Comparable<? super T>> : " + nb.ranking());

        // Comparator<Team> reused for a board of CollegeTeam (Comparator<? super T>)
        Leaderboard<CollegeTeam> colleges = new Leaderboard<>(BY_RANK);
        colleges.add(new CollegeTeam("SIMATS", 7, 2));
        colleges.add(new CollegeTeam("VIT", 9, 1));
        colleges.add(new CollegeTeam("SRM", 7, 5));
        System.out.println("Leaderboard<CollegeTeam> using Comparator<Team>  : " + colleges.ranking());

        // ---------- alternatives: Comparable vs 3 stable sorts vs comparator chain ----------
        System.out.println();
        System.out.println("=== Alternative designs on n random teams (average of 5 runs, ms) ===");
        PrintWriter csv = new PrintWriter("q5_sort_results.csv");
        csv.println("n,comparator_chain_1sort_ms,comparable_1sort_ms,three_stable_sorts_ms,same_result");
        System.out.printf("%10s %22s %18s %22s %12s%n", "n", "Comparator chain(1)", "Comparable(1)", "3 stable sorts(3)", "same order");
        for (int w = 0; w < 3; w++) compareDesigns(20000, new Random(w));
        for (int n : new int[]{10000, 100000, 500000}) {
            double[] tot = new double[3];
            boolean same = true;
            for (int r = 0; r < 5; r++) {
                Object[] res = compareDesigns(n, new Random(100 + r));
                double[] t = (double[]) res[0];
                for (int k = 0; k < 3; k++) tot[k] += t[k];
                same &= (Boolean) res[1];
            }
            System.out.printf("%10d %22.2f %18.2f %22.2f %12b%n", n, tot[0] / 5, tot[1] / 5, tot[2] / 5, same);
            csv.printf("%d,%.3f,%.3f,%.3f,%b%n", n, tot[0] / 5, tot[1] / 5, tot[2] / 5, same);
        }
        csv.close();
    }

    static Object[] compareDesigns(int n, Random rnd) {
        List<Team> base = new ArrayList<>();
        List<ComparableTeam> comp = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String name = "T" + rnd.nextInt(n);
            int p = rnd.nextInt(40), g = rnd.nextInt(30) - 15;      // many ties
            base.add(new Team(name, p, g));
            comp.add(new ComparableTeam(name, p, g));
        }
        double[] t = new double[3];

        List<Team> l1 = new ArrayList<>(base);
        long s = System.nanoTime();
        l1.sort(BY_RANK);                                           // one sort, comparator chain
        t[0] = (System.nanoTime() - s) / 1e6;

        List<ComparableTeam> l2 = new ArrayList<>(comp);
        s = System.nanoTime();
        Collections.sort(l2);                                       // one sort, compareTo
        t[1] = (System.nanoTime() - s) / 1e6;

        List<Team> l3 = new ArrayList<>(base);
        s = System.nanoTime();
        l3.sort(Comparator.comparing((Team x) -> x.name));                     // sort 1 : least important key
        l3.sort(Comparator.comparingInt((Team x) -> x.goalDiff).reversed());   // sort 2
        l3.sort(Comparator.comparingInt((Team x) -> x.points).reversed());     // sort 3 : most important key
        t[2] = (System.nanoTime() - s) / 1e6;

        boolean same = l1.toString().equals(l2.toString()) && l1.toString().equals(l3.toString());
        return new Object[]{t, same};
    }
}
