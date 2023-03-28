#!/usr/bin/env bash -e



################################################
############ Somewear Build Script #############
################################################
version=${VERSION_NAME}
_profile=Release


./gradlew clean assemble${_profile} -x test -PVERSION_NAME=${version}
outputDir="./build/outputs/aar"
mkdir -p $outputDir
aarFilePath="$outputDir/logback-android-release.aar"
cp -vf ./logback-android/build/outputs/aar/logback-android*.aar $aarFilePath
echo "Moved aar file to $aarFilePath"


################################################
############ Original build script #############
################################################

#if [[ ! -z "$1" ]] && [[ "$1" != -r ]]; then
#  echo "Creates the uber jar in ./build"
#  echo
#  echo " Usage: $0 [-r]"
#  echo
#  echo " Options:"
#  echo "   -r  make release build (default: false)"
#  exit 1
#fi

#. gradle.properties
#
#version=${VERSION_NAME}
#_profile=Debug
#if [[ "$1" == "-r" ]]; then
#  _profile=Release
#  version=${version%-SNAPSHOT}
#fi
#
#./gradlew clean assemble${_profile} -x test -PVERSION_NAME=${version}
#mkdir build
#cp -vf ./logback-android/build/outputs/aar/logback-android*.aar ./build/

