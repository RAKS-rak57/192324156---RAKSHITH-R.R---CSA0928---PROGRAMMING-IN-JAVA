// Expected: compile error - @Override catches the typo ratePerHr()
abstract class V { protected abstract double ratePerHour(); }
class Bad extends V {
    @Override protected double ratePerHr() { return 10; }
    protected double ratePerHour() { return 1; }
}
