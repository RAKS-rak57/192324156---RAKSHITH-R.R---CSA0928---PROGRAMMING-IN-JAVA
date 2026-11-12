#!/usr/bin/env bash
# Scripted jdb session: breakpoints on the close() line and on the failing throw, run from the Q3 folder after compiling with javac -g
(sleep 2; for c in "stop at ReportGenerator:53" "stop at ReportGenerator:34" "run" "print f.getName()" "print Tracked.OPEN.size()" "cont" "print f.getName()" "print Tracked.OPEN.size()" "cont" "where" "locals" "print Tracked.OPEN.size()" "cont" "cont" "quit"; do echo "$c"; sleep 1.2; done) | timeout 60 jdb -sourcepath src -classpath out LeakDebugTarget 2>&1 | sed 's/\r//g' | grep -v "^$"
