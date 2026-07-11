#!/usr/bin/env bash
set -e

if [[ ! -z "$1" ]] && [[ "$1" != -r ]]; then
  echo "Creates the AAR in ./build"
  echo
  echo " Usage: $0 [-r]"
  echo
  echo " Options:"
  echo "   -r  make release build (default: false)"
  exit 1
fi

# Read VERSION_NAME from gradle.properties. Do NOT source the file: it is a
# Java properties file, and lines like "org.gradle.jvmargs=-Xmx6g" are not
# valid shell (issue #346).
version=$(sed -n 's/^VERSION_NAME=//p' gradle.properties | tr -d '[:space:]')
if [[ -z "$version" ]]; then
  echo "error: VERSION_NAME not found in gradle.properties" >&2
  exit 1
fi

_profile=Debug
if [[ "$1" == "-r" ]]; then
  _profile=Release
  version=${version%-SNAPSHOT}
fi

./gradlew clean assemble${_profile} -x test -PVERSION_NAME=${version}
mkdir -p build
cp -vf ./logback-android/build/outputs/aar/logback-android*.aar ./build/
