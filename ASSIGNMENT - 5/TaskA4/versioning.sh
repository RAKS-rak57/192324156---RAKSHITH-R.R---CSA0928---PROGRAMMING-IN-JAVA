#!/bin/sh
# Task A4.2: write with version 1 of the classes, read with version 2.
JC="java -m jdk.compiler/com.sun.tools.javac.Main"
rm -rf out_v1 out_v2; mkdir out_v1 out_v2
$JC -d out_v1 v1/*.java && $JC -d out_v2 v2/*.java || exit 1
java -cp out_v1 Writer
java -cp out_v2 Reader
rm -f fixed_v1.ser implicit_v1.ser
