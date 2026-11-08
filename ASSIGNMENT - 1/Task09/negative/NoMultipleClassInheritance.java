// 1) A class can extend only ONE class, so "Chargeable as an abstract class" blocks ElectricCar from also extending Car.
abstract class ChargeableA { abstract double chargingFee(int kWh); }
class CarX { }
class ElectricCarX extends CarX, ChargeableA { }
