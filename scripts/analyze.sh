#!/usr/bin/env bash

# Run Jacoco separately from check or else any analysis violations
# cause a break in the gradle pipeline, preventing Jacoco from running.
./gradlew -Panalysis clean jacoco
./gradlew -Panalysis check || true

echo
echo Reports:
echo

BASE_PATH=logback-android/build/reports

reports="
checkstyle/main.html
findbugs/release.html
pmd/main.html
jacoco/jacocoTestReport/html/index.html
"
for f in ${reports}
do
    [ -f "${BASE_PATH}/$f" ] && echo file://${PWD}/${BASE_PATH}/$f
done
