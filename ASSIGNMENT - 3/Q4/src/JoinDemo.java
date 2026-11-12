/** Student A: join() makes main wait, so the total is printed after every station has finished. */
public class JoinDemo {
    static volatile int total = 0;
    public static void main(String[] args) throws Exception {
        System.out.println("--- without join() ---");
        total = 0;
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) { ts[i] = new Thread(() -> { int n = Scanner1.scan("x", 20); synchronized (JoinDemo.class) { total += n; } }); ts[i].start(); }
        System.out.println("total printed immediately by main : " + total + "   (WRONG - stations still scanning)");
        for (Thread t : ts) t.join();
        System.out.println("total after the stations finished  : " + total);
        System.out.println("--- with join() ---");
        total = 0;
        for (int i = 0; i < 4; i++) { ts[i] = new Thread(() -> { int n = Scanner1.scan("x", 20); synchronized (JoinDemo.class) { total += n; } }); ts[i].start(); }
        for (Thread t : ts) t.join();                                   // main is WAITING here until the thread terminates
        System.out.println("total printed after join()          : " + total + "   (correct: 4 x 20 = 80)");
    }
}
