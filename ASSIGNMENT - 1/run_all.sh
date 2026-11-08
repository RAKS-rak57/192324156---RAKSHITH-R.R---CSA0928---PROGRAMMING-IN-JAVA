#!/usr/bin/env bash
# Compiles and runs every task. Usage: ./run_all.sh   (needs JDK 17+; developed with OpenJDK 21)
set -e
run() { echo; echo "================ $1 ================"; (cd "$1" && shift && eval "$@"); }
J="java -cp out"
run Task01 "javac -d out src/Task01Demo.java && $J Task01Demo"
run Task02 "javac -d out src/Task02Demo.java && $J Task02Demo < input/input.txt"
run Task03 "javac -d out src/*.java && $J ParkingFacility && $J FastParkingFacility && $J FeeCalculator && $J AllocBenchmark"
run Task04 "javac -d out src/*.java && $J Task04Demo"
run Task05 "javac -d out src/*.java && $J Task05Demo"
run Task06 "javac -d out src/*.java && $J Task06Demo"
run Task07 "javac -d out src/*.java && $J Task07Demo"
run Task08 "javac -d out src/FeeDemo.java src/poly/PolymorphismDemo.java && $J FeeDemo && $J poly.PolymorphismDemo"
run Task09 "javac -d out src/*.java && $J Task09Demo"
run Task10 'javac -d out $(find src -name "*.java") && java -cp out parking.app.Main'
run Task11 'javac -d out $(find src -name "*.java") && java -cp out parking.app.TestRunner && java -cp out parking.app.Main < input/commands.txt && java -cp out parking.app.LookupBenchmark'
run Task12 'javac -d out $(find src -name "*.java") && java -cp out parking.app.TestRunner && java -cp out parking.app.Task12Tests && java -cp out parking.app.Main < input/commands.txt'
