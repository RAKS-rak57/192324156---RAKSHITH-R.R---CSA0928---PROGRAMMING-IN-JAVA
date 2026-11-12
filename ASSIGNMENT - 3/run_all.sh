#!/usr/bin/env bash
# Compiles and runs every question. Usage: ./run_all.sh   (JDK 17+; developed with OpenJDK 21)
# Note: Q4-Q7 contain timing experiments; they take a few minutes. Numbers depend on YOUR machine (cores!).
set -e
J="java -cp out"
sect() { echo; echo "================ $1 ================"; }
sect Q1; (cd Q1 && javac -g -d out src/*.java && $J AtmTest && $J UncaughtDemo || true; $J ErrorDemo; $J ExceptionCostBenchmark)
sect Q2; (cd Q2 && javac -g -d out src/StorageDemo.java src/portal/*.java src/portal_unchecked/*.java && javac -g -d out_nocause src/StorageDemoNoCause.java && javac -g -cp out -d out src/StackTraceCostBenchmark.java \
        && $J StorageDemo && (java -cp out_nocause StorageDemoNoCause || true) && $J portal.PortalMain && $J portal.ChainDemo && $J StackTraceCostBenchmark)
sect Q3; (cd Q3 && javac -g -d out src/*.java && $J ReportExperiment && $J TwrDemo && $J ReportScaleBenchmark && bash debug_session.sh)
sect Q4; (cd Q4 && javac -g -d out src/*.java && $J StationsMain thread && bash run_ten.sh && $J LifeCycleDemo && $J RunVsStart && $J JoinDemo && $J PriorityExperiment && $J DaemonDemo daemon && $J ExecutorStations)
sect Q5; (cd Q5 && javac -g -d out src/*.java && $J SeatBooking && for i in 1 2 3 4 5 6 7 8 9 10; do $J SeatBookingUnsync; done && $J RaceReproducer && $J StaticSyncDemo && $J BookingBenchmark)
sect Q6; (cd Q6 && javac -g -d out src/*.java && $J TokenCounter && $J CounterTrace && $J CounterStress && $J SleepVsWait && $J DeadlockDemo && $J DeadlockFixed && $J TokenQueueDemo && $J Q6Benchmark)
sect Q7; (cd Q7 && javac -g -d out $(find src -name "*.java") && $J results.Main input/handset 4 && $J results.FaultInjectionTest \
        && $J results.GenerateScores 10000 data && $J results.Main data 4 && $J results.ScalingExperiment data \
        && $J results.Alternatives parstream data 500 && $J results.Alternatives parstream-naive data 0 && $J results.Alternatives thread-per-file data 500)
