# CSA0928 Programming in Java – Assignment 1: Campus Smart-Parking System

| | |
|---|---|
| **Name** | [ENTER YOUR NAME] |
| **Register number** | [ENTER REGISTER NUMBER] |
| **Course** | CSA0928 – Programming in Java (CO1) |
| **Java version** | OpenJDK 21 (`java -version`; any JDK 17+ works because `instanceof` patterns and records are used) |
| **Repository** | CSA09-[RegisterNumber]-Assignment1 |

## Layout
Every task has its own folder: `src/` (Java source), `input/` (input files), `output/` (program output as `.txt` and screenshot `.png`).
UML diagrams are in `docs/uml/`. Task 7, 9 and 10 also have a `negative/` folder with deliberately wrong code that shows the compiler errors (see `output/compile_errors.txt`).

Run everything: `./run_all.sh`. Or one task at a time as below (run the commands inside the task folder).

## How to compile and run each task, and the result

| Task | Compile | Run | Result |
|---|---|---|---|
| 01 Objects vs parallel arrays | `javac -d out src/Task01Demo.java` | `java -cp out Task01Demo` | Both searches O(n); object design retained (maintainability) |
| 02 Data representation | `javac -d out src/Task02Demo.java` | `java -cp out Task02Demo < input/input.txt` | Integer division bills Rs 60 instead of Rs 90 for 130 min; `(minutes+59)/60` fixes it; jagged array chosen |
| 03 Control logic – **Complete the Code** | `javac -d out src/*.java` | `java -cp out ParkingFacility` (also `FastParkingFacility`, `FeeCalculator`, `AllocBenchmark`) | Output equals the expected output: `[0, 0] [0, 1] [1, 0] null / true false false / [0, 1]` |
| 04 Classes, constructors | `javac -d out src/*.java` | `java -cp out Task04Demo` | Chaining, copy constructor (duplicate ticket), setters/factory comparison |
| 05 static, life cycle | `javac -d out src/*.java` | `java -cp out Task05Demo` | Static counter keeps ticket numbers unique across two facilities; ticket garbage-collected after exit |
| 06 Inheritance | `javac -d out src/*.java` | `java -cp out Task06Demo` | Constructor order Vehicle → Car → ElectricCar traced; composition alternative |
| 07 Overriding | `javac -d out src/*.java` | `java -cp out Task07Demo` | equals/hashCode by regNo; `final`; override-rule compiler errors |
| 08 Polymorphism – **Complete the Code** | `javac -d out src/FeeDemo.java src/poly/PolymorphismDemo.java` | `java -cp out FeeDemo` and `java -cp out poly.PolymorphismDemo` | FeeDemo output equals expected: Rs 30.0 / 90.0 / 210.0 / true |
| 09 Abstraction | `javac -d out src/*.java` | `java -cp out Task09Demo` | Abstract class + interfaces; interface-based alternative cannot hold state or a constructor |
| 10 Packages, encapsulation | `javac -d out $(find src -name "*.java")` | `java -cp out parking.app.Main` | Validating setters reject bad data; wrapper `==` pitfall shown |
| 11 Implementation, testing | `javac -d out $(find src -name "*.java")` | `java -cp out parking.app.TestRunner`, `java -cp out parking.app.Main < input/commands.txt`, `java -cp out parking.app.LookupBenchmark` | 35 / 35 tests pass; hand-calculated day Rs 500 = program output |
| 12 Design challenge | `javac -d out $(find src -name "*.java")` | `java -cp out parking.app.TestRunner` (35 original tests), `parking.app.Task12Tests`, `parking.app.Main < input/commands.txt` | 35 / 35 and 31 / 31 pass; hand-calculated day Rs 285 = program output; 4 of 14 classes modified |

On Windows replace `$(find src -name "*.java")` with `src\parking\model\*.java src\parking\service\*.java src\parking\app\*.java`.

## Report
The written report (`Assignment1_Report.docx`) contains the analysis, UML diagrams, pseudocode, test tables and complexity analyses for Tasks 1–12.
