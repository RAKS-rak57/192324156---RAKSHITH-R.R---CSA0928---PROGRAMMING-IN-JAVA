#!/bin/sh
# Usage: tools/run_headless.sh <task-folder> <MainClass> [args...]
# Compiles the task's .java files (plus the harness helpers) against the headless shims and runs MainClass.
# This is ONLY for machines without a display. On a normal PC just use javac / java.
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SH=$ROOT/tools/headless-shims
SO=$ROOT/tools/.shimout
JC="java -m jdk.compiler/com.sun.tools.javac.Main"
if [ ! -d "$SO" ]; then
  mkdir -p "$SO"
  $JC --patch-module java.desktop=$SH -d "$SO" $(find $SH -name '*.java') || exit 1
fi
TASK=$1; MAIN=$2; shift 2
OUT=$ROOT/$TASK/.out_headless
rm -rf "$OUT"; mkdir -p "$OUT"
$JC --patch-module java.desktop=$SO -d "$OUT" $(find $ROOT/$TASK -name '*.java' -not -path '*/errors/*') $ROOT/tools/harness/*.java 2>&1 | grep -v "^Note:" 
cd $ROOT/$TASK
java --patch-module java.desktop=$SO -Djava.awt.headless=true -cp "$OUT" "$MAIN" "$@"
