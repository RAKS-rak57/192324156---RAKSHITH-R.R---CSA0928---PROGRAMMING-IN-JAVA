# CSA09 – Programming in Java – Assignment 4 (CO4): Event-Driven GUI Design

| | |
|---|---|
| Name | <YOUR NAME> |
| Register number | <REGISTER NUMBER> |
| Java version | OpenJDK 21 (check with `java -version`) |
| Repository | CSA09-<RegisterNumber>-Assignment4 |

One folder per task: Java source, SQL scripts, captured console output (`*_output.txt`), CSV measurements and screenshots
(`screenshots/taskNN/`). The report is in `report/`. Diagrams (ER, class, container, flow) are in `diagrams/`.

## How to compile and run (on a computer with a display)
Open a terminal in the task folder.

| Task | Commands | Result (short note) |
|---|---|---|
| 1 | `cd Task01` · `javac *.java` (exclude `FeeHarness.java`) · `java FeeServiceTest` · `java -Dfee.demo=true FeeWindow` | Service tests 12/12 pass. Window works without a database in demo mode; MySQL: `-Dfee.url=... -Dfee.user=... -Dfee.pwd=...` and put `mysql-connector-j-8.x.jar` on the class path. |
| 2 | `cd Task02` · `javac *.java` · `java AppointmentServiceTest` · `java AppointmentWindow` · `java WindowTrace` | Doctors cached in a Map; window-event trace printed by `WindowTrace`. |
| 3 | `cd Task03` · `javac *.java` · `java LibraryFrame nested` (or `gridbag`, `absolute`) | Resize the window: results grow, search row and buttons keep their size (nested design). |
| 4 | `cd Task04` · `javac DrawPad*.java` · `java DrawPad` · `javac errors/DrawPadWithOverride.java` (shows the compiler error) | Dots survive minimise/restore. |
| 5 | `cd Task05` · `javac Converter*.java` · `java Converter` (and `ConverterA` … `ConverterF`) | Convert button and Enter key use one lambda listener. |
| 6 | `cd Task06` · create the database: `mysql -u root -p < sql/campus_events.sql` · `javac -d out $(find src -name "*.java")` · `java -Ddemo=true -cp out campus.reg.App` (no database) or `java -Ddb.url=jdbc:mysql://localhost:3306/campus_events -Ddb.user=root -Ddb.pwd=... -cp "out;mysql-connector-j-8.x.jar" campus.reg.App` | Register, view, filter, page and export registrations. |

(On Linux / macOS use `:` instead of `;` in the class path.)

## Notes about the evidence in this repository
* `tools/headless-shims`, `tools/harness`, `tools/run_headless.sh`: a test harness that runs the real GUI classes **without a display**
  and renders them to PNG. The screenshots in `screenshots/` were produced this way (off-screen renders). Retake real screenshots on your own
  computer for the final submission.
* `Task06/sql/sql_logic_check.py` checks the SQL logic with SQLite (no MySQL server was available); run `campus_events.sql` in MySQL as well.
