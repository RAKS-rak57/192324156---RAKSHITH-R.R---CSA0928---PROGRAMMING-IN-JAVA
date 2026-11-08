// Expected: compile error - overriding method may not reduce visibility (public -> protected)
class P { public double fee(int m) { return m; } }
class C extends P { @Override protected double fee(int m) { return m; } }
