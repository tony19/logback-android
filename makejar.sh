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
gradle clean shadowJar -Pver=${version} -P${profile}

# FIXME: Currently applying shadowJar from logback-classic in order
# to include all required dependencies.
mv -f logback-classic/build/libs/logback-android-classic-${version}-all.jar \
      build/libs/logback-android-${version}.jar

echo "Created ./build/libs/logback-android-${version}.jar"
