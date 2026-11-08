// Expected: compile errors - cannot override a final method, cannot reassign a final field
class A { public final String id() { return "A"; } private final int x = 1; void setX() { x = 2; } }
class B extends A { @Override public String id() { return "B"; } }
