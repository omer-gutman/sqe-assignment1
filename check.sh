#!/usr/bin/env bash
# Runs the course mini-grader on our TestLibrary.java and prints the grade.
# The grader needs Java 17 (newer versions break its coverage tool).
cd "$(dirname "$0")"

# On macOS, use Java 17 automatically if it's installed, whatever the default is.
JAVA=java
if [ -x /usr/libexec/java_home ] && J17=$(/usr/libexec/java_home -v 17 2>/dev/null); then
  # Child processes the grader starts must use 17 too, so set JAVA_HOME and PATH.
  export JAVA_HOME="$J17"
  export PATH="$J17/bin:$PATH"
  JAVA="$J17/bin/java"
fi
if ! "$JAVA" -version 2>&1 | grep -q '"17'; then
  echo "Java 17 not found. Install it (see README) and run again."
  exit 1
fi

TEST=Assignment-1-code/src/test/java/ac/il/bgu/qa/TestLibrary.java
"$JAVA" -jar assignment1.jar -t "$TEST"
echo "-----"
cat "${TEST%.java}.grade"
