public class TokenCounterIf {
    private final int[] buffer = new int[5]; // the counter holds at most 5 tokens
    private int count = 0, in = 0, out = 0;

    public synchronized void put(int token) throws InterruptedException {
        // TODO 1: Wait while the counter is full, add the token at position in, then
        // wake waiting threads
        if (count == buffer.length) wait();      // loop: the condition is re-tested after every wake-up
        buffer[in] = token;
        in = (in + 1) % buffer.length;
        count++;
        notifyAll();
    }

    public synchronized int take() throws InterruptedException {
        // TODO 2: Wait while the counter is empty, remove the token at position out,
        // wake waiting threads and return the token
        if (count == 0) wait();
        int token = buffer[out];
        out = (out + 1) % buffer.length;
        count--;
        notifyAll();
        return token;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounterIf counter = new TokenCounterIf();
        int[] collected = new int[2];
        Thread cook = new Thread(() -> {
            try { for (int t = 1; t <= 20; t++) counter.put(t); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try { for (int k = 0; k < 10; k++) collected[id] += counter.take(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });   // NOTE: the printed assignment code is missing the closing brace of this lambda; added here
        }
        cook.start();
        for (Thread t : students) t.start();
        cook.join();
        for (Thread t : students) t.join();
        System.out.println("Sum of tokens collected: " + (collected[0] + collected[1])
                + " (expected 210)");
    }
}
