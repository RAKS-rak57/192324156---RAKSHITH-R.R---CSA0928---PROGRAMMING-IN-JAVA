#!/bin/sh
# Usage: tools/run_jdbc.sh <task-folder> <MainClass> [args...]
# Compiles the task's sources together with the test-only SQLite JDBC shim and runs MainClass (Java 21, preview API).
# On a computer with MySQL you do NOT need this: use javac/java with mysql-connector-j on the class path.
ROOT=$(cd "$(dirname "$0")/.." && pwd)
TASK=$1; MAIN=$2; shift 2
OUT=$ROOT/$TASK/.out_jdbc
rm -rf "$OUT"; mkdir -p "$OUT"
java -m jdk.compiler/com.sun.tools.javac.Main -source 21 --enable-preview -sourcepath "$ROOT/TaskB1" -d "$OUT" $(find $ROOT/tools/sqlite-jdbc-shim/src $ROOT/$TASK -name '*.java' -not -path '*/.out_*' -not -path '*/v1/*' -not -path '*/v2/*' -not -path '*/errors/*') 2>&1 | grep -v "^Note:"
cd $ROOT/$TASK
java --enable-preview --enable-native-access=ALL-UNNAMED -cp "$OUT" "$MAIN" "$@" 2>&1 | grep -v "^WARNING:"
