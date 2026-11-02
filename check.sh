#!/usr/bin/env bash
# Runs the course mini-grader on our TestLibrary.java and prints the grade.
set -e
cd "$(dirname "$0")"
TEST=Assignment-1-code/src/test/java/ac/il/bgu/qa/TestLibrary.java
java -jar assignment1.jar -t "$TEST"
echo "-----"
cat "${TEST%.java}.grade"
