#!/bin/sh -e

if [[ ! -z "$1" ]] && [[ "$1" != -r ]]; then
  echo "Creates the uber jar in ./build"
  echo
  echo " Usage: $0 [-r]"
  echo
  echo " Options:"
  echo "   -r  make release build (default: false)"
  exit 1
fi

. gradle.properties

_profile="debug"
if [[ "$1" == "-r" ]]; then
  _profile="release"
  version=${version%-SNAPSHOT}
fi

./gradlew clean uberjar -x test -Pversion=${version} -P${_profile}

echo "created  $PWD/build/logback-android-${version}.jar"
