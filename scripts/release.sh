#!/bin/sh -e

. gradle.properties
version=${baseVersion}-${buildVersion}

echo "Starting release process for logback-android ${version}..."

./gradlew -Pversion=${version}  \
            -Ppush              \
            clean               \
            readme              \
            release             \
            uploadArchives      \
            uploadDocs          \
            uberjar

hub release edit -d -m '' ${version} -a build/logback-android-${version}.jar
