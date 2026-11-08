// 3) A field declared in an interface is a constant (public static final); per-object regNo cannot live there.
interface VehicleI { String regNo = "X"; }
class Assign implements VehicleI { void set() { regNo = "Y"; } }
