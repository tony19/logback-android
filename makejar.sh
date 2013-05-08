#!/bin/sh
#
# This script creates the uber jar (with debug symbols). The tests are skipped.
# The jar will be stored in the "./target" directory:
#
#   ./target/logback-android-<version>.jar
#

# Usage: ./makejar.sh [-r]
#
#  Options:
#
#  -r   make release build (default: false)
#
profile="debug"

if [ "x$1" == "x-r" ];
then
  profile="release"
fi

set -e

mvn -P $profile clean install -DskipTests=true
mvn -f pom-uber.xml package
