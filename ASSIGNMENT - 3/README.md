# CSA0928 Programming in Java – Assignment 3 (CO3): Exceptions and Multithreading

| | |
|---|---|
| **Student A** | [NAME] – register number [REGISTER NUMBER] |
| **Student B** | [NAME] – register number [REGISTER NUMBER] |
| **Java version** | OpenJDK 21 (any JDK 17+ works) |
| **Repository** | CSA09-[RegisterNumber]-Assignment3 |

Folders `Q1` … `Q7`: `src/` (Java source), `input/` (input files), `output/` (program output as `.txt` + screenshot `.png`).
`docs/diagrams` = UML/state diagrams, `docs/charts` = graphs of the measurements. Run everything with `./run_all.sh`
(inside a folder: compile with `javac -g -d out src/*.java`, run with `java -cp out <MainClass>`).

| Q | Program(s) to run (inside the folder) | Result |
|---|---|---|
| Q1 ATM | `AtmTest`, `UncaughtDemo`, `ErrorDemo`, `ExceptionCostBenchmark`; `negative/` = compile-error evidence | 12/12 tests; every path executes `finally`; exception vs error code measured |
| Q2 Portal | `StorageDemo` (**Complete the Code**), `StorageDemoNoCause`, `portal.PortalMain`, `portal.ChainDemo`, `StackTraceCostBenchmark` | StorageDemo output equals the expected output; 8/8 scenarios; chaining evidence |
| Q3 Reports | `ReportExperiment`, `TwrDemo`, `ReportScaleBenchmark`, `bash debug_session.sh` (jdb) | leaky version leaves 2 handles open; manual `finally` and try-with-resources leave 0 |
| Q4 Threads | `StationsMain thread\|runnable`, `bash run_ten.sh`, `LifeCycleDemo`, `RunVsStart`, `JoinDemo`, `PriorityExperiment`, `DaemonDemo [daemon]`, `ExecutorStations` | 24 recorded runs; pool follows n/p model |
| Q5 Cinema | `SeatBooking` (**Complete the Code**), `SeatBookingUnsync`, `SeatBookingUnsyncSlow`, `RaceReproducer`, `BookingBenchmark`, `StaticSyncDemo` | `Seats sold: 10`; double booking reproduced by widening the race window |
| Q6 Wallet + tokens | `TokenCounter` (**Complete the Code**), `TokenCounterIf`, `CounterTrace`, `CounterStress`, `SleepVsWait`, `DeadlockDemo`, `DeadlockFixed`, `TokenQueueDemo`, `Q6Benchmark` | `Sum ... 210`; `if` and `notify()` failures shown; deadlock detected and prevented |
| Q7 Results | `results.Main input/handset 4`, `results.FaultInjectionTest`, `results.GenerateScores 10000 data` then `results.Main data 4`, `results.ScalingExperiment data`, `results.Alternatives ...` | hand-checked set matches; 10,000-file summary equals the ground truth |

**Note about the printed `TokenCounter` code:** the lambda of the student threads is missing one closing brace in the assignment text; it was added so that the program compiles. Nothing else was changed.
The measurements were taken on a 1-core machine; re-run them on yours and compare (see the report).
