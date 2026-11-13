# CSA09 – Programming in Java – Assignment 5 (CO5): Persistence – Files, Streams, Serialization, JDBC

| | |
|---|---|
| Name | <YOUR NAME> |
| Register number | <REGISTER NUMBER> |
| Java version | OpenJDK 21 (`java -version`) |
| Repository | CSA09-<RegisterNumber>-Assignment5 |

One folder per task (Part A: TaskA1–A5, Part B: TaskB1–B4): Java source, SQL scripts, captured console output (`*_output*.txt`),
CSV measurements, and screenshots (`screenshots/`). The report with the marks map is in `report/`.
Part A answers (design and pseudocode) are in the report; TaskA3–A5 hold the supporting programs.

## How to compile and run
| Task | Commands (from the task folder) | Result (short note) |
|---|---|---|
| B1 | `javac *.java` · `java PassStore` · `java BusPassStoreTest` · `java BufferingBenchmark` · `java GrowthBenchmark` | PassStore prints `[P1003, 192311003, Divya, 7, 3800.0]` then `null`; 11/11 tests pass; 50 MB: 52,428,784 reads unbuffered vs 6,401 buffered |
| B2 | needs a JDBC database (see below) · `java AdversarialDemo` · `java DamagedFileDemo` | 3 attacks fool the string-built query; PreparedStatement rejects all; 9 damaged lines reported |
| B3 | `java B3Correctness` · `java LoaderBenchmark <dir> 1000,10000,100000 both loader_results.csv` · `java FailureDemo` | one transaction: 0 rows after a failure; auto-commit leaves 500; transaction is ~260x faster for 100,000 rows |
| B4 | `java MigrationDemo` · `java ScaleBenchmark` · `java ConcurrencyDemo` | 8,014 lines → 8,000 loaded + 14 rejected with reasons; database lookup 0.025 ms vs 781 ms (text file) |
| A3/A4/A5 | `java A3Demo` · `java SerializationDemo` · `./versioning.sh` · `java FormatBenchmark` · `java TransferDemo` | supporting programs of the Part A answers |

## Database
* **With MySQL (use this for the submission):** run the `.sql` scripts, put `mysql-connector-j-8.x.jar` on the class path, remove the line
  `minisqlite.MiniSqlite.install();` and use a `jdbc:mysql://localhost:3306/<db>` URL (see Appendix A of the report).
* **Without MySQL (how the evidence in this repository was produced):** `tools/run_jdbc.sh <TaskFolder> <MainClass> [args]` compiles the task with a
  test-only JDBC driver for SQLite (`tools/sqlite-jdbc-shim`, Java 21 foreign-function API). Real SQLite engine, JDBC interfaces only.
