import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** Alternative: the same mess counter with an ArrayBlockingQueue of capacity 5 - no wait/notify written by hand. */
public class TokenQueueDemo {
    public static void main(String[] args) throws Exception {
        BlockingQueue<Integer> counter = new ArrayBlockingQueue<>(5);
        int[] collected = new int[2];
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 20; t++) counter.put(t); } catch (InterruptedException e) { } });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) { final int id = s; students[s] = new Thread(() -> { try { for (int k = 0; k < 10; k++) collected[id] += counter.take(); } catch (InterruptedException e) { } }); }
        cook.start(); for (Thread t : students) t.start();
        cook.join(); for (Thread t : students) t.join();
        System.out.println("Sum of tokens collected: " + (collected[0] + collected[1]) + " (expected 210)");
    }
}
