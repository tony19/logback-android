#!/usr/bin/env bash -e

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

version=${VERSION_NAME}
_profile=Debug
if [[ "$1" == "-r" ]]; then
  _profile=Release
  version=${version%-SNAPSHOT}
fi

./gradlew clean assemble${_profile} -x test -PVERSION_NAME=${version}
cp -vf ./logback-android/build/outputs/aar/logback-android*.aar ./build/
