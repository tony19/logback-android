#!/bin/sh
#
# This script creates the uber jar (with debug symbols). The tests are skipped.
# The jar will be stored in the "./build/libs" directory:
#
#   ./build/libs/logback-android-<version>.jar
#

# Usage: ./makejar.sh [-r]
#
#  Options:
#
#  -r   make release build (default: false)
#

if [ ! $1 ]; then
  echo "Usage: $0 [-r]"
  echo
  echo "Options:"
  echo "  -r  make release build (default: false)"
  exit 1
fi

. gradle.properties
_profile="debug"

if [ "x$1" == "x-r" ]
then
  _profile="release"
  version=${baseVersion}-${buildVersion}
fi

set -e

./gradlew clean shadowJar -Pversion=${version} -P${_profile}

# FIXME: Currently applying shadowJar from logback-classic in order
# to include all required dependencies, so we need to copy it from
# logback-classic's output directory into the main output.
mkdir -p build/libs
mv -f logback-classic/build/libs/logback-android-${version}-all.jar \
      build/libs/logback-android-${version}.jar

echo "Created ./build/libs/logback-android-${version}.jar"
