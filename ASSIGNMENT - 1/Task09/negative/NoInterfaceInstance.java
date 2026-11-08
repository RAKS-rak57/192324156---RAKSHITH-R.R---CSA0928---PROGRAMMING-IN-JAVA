// 2) An interface has no constructor and no instance state.
interface VehicleI { String regNo = "X"; }
class UseIt { void f() { VehicleI v = new VehicleI(); } }
