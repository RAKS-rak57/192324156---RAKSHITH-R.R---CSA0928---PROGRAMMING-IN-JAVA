public class SeatBookingUnsyncSlow {
    private final boolean[] booked;
    private int bookingsMade = 0;

    SeatBookingUnsyncSlow(int seats) { booked = new boolean[seats]; }

    // TODO 1: Make the check-then-book operation atomic so that a seat can never be
    // sold twice
    void book(int seat) {
        if (!booked[seat]) {          // check ...
            Thread.yield();           // another thread may run RIGHT HERE
            booked[seat] = true;      // ... then act: both steps now execute as ONE indivisible unit
            bookingsMade++;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBookingUnsyncSlow screen = new SeatBookingUnsyncSlow(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10; // 10 users compete for every seat
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}
