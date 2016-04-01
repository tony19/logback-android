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

if [ "x$1" == "x-r" ]
then
  profile="release"
fi

set -e

. gradle.properties
version=${baseVersion}-$((${buildVersion} + 1))
gradle clean build jar uberjar -Pver=${version} -P${profile}
echo "Created ./build/libs/logback-android-${version}.jar"
