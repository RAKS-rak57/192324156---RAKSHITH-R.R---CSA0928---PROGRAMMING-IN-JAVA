# CSA09 – Programming in Java – Assignment 2 (CO2)

| | |
|---|---|
| Name | <YOUR NAME> |
| Register number | <REGISTER NUMBER> |
| Java version | OpenJDK 21 (check with `java -version`) |
| Repository | CSA09-<RegisterNumber>-Assignment2 |

Each question has its own folder containing the Java source, the CSV files with the raw measurements,
the console output (`output.txt`) and output screenshots. The full report is in `report/`.

## How to compile and run
Open a terminal inside the folder of the question.

| Task | Commands | Result (short note) |
|---|---|---|
| Q1 | `cd Q1` · `javac TicketBuilder.java` · `java TicketBuilder` | `String +` is O(n²): about 270 times slower than StringBuilder for 20 tickets of 5,000 fields. StringBuffer is correct for a shared log; StringBuilder lost data in some trials. |
| Q2 | `cd Q2` · `javac PlaylistExperiments.java` · `java PlaylistExperiments` | ArrayList is best for get(k) and appends; LinkedList only wins at the front; ArrayDeque is best for the "Up Next" queue. |
| Q3 | `cd Q3` · `javac *.java` · `java AttendanceBefore` · `java Attendance` · `java HashContractExperiments` | Without hashCode: `Unique students: 3`. With TODO 1 done: `Unique students: 2`. |
| Q4 | `cd Q4` · `javac PharmacyInventory.java` · `java PharmacyInventory` | HashMap/LinkedHashMap lookups are O(1), TreeMap O(log n). Each map is cheapest for its own natural report. Chosen: LinkedHashMap. |
| Q5 | `cd Q5` · `javac *.java` · `java LeaderboardDemo` · `java DesignTests` | `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]`. Comparator chain passes all consistency checks; the subtraction comparator fails. |
| Q6 | `cd Q6` · `javac *.java` · `java Triage` · `java TriageExperiments` | `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)`. Aging rule fixes starvation. |
| Q7 | `cd Q7` · `javac TriageFair.java` · `java TriageFair` | Fair triage redesigned from O(n²) total to O(1) per serve; identical order in 300 random tests. |

## Other folders
* `graphs/` – figures used in the report (`python3 scripts/make_graphs.py`)
* `scripts/` – graph / screenshot generators
* `report/` – assignment report
