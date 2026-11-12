#!/usr/bin/env bash
# Student B: run the same program in 12 separate JVMs and record the finish order each time.
for mode in thread runnable; do
  echo "== mode: $mode =="
  for i in $(seq 1 12); do printf "run %2d: " $i; java -cp out StationsMain $mode | tail -1; done
done
