#!/usr/bin/env bash -e

. gradle.properties

version=${version%*-SNAPSHOT}
baseVersion=${version%*-*}
nextBuild=$((${version##*-} + 1))
nextVersion="${baseVersion}-${nextBuild}-SNAPSHOT"

echo "Starting release for logback-android ${version}..."

# gradle-release-plugin prompts for your Nexus credentials
# with "Please specify username" (no mention of Nexus).
# Use our own prompt to remind the user where they're
# logging into to.
user=${NEXUS_USERNAME}
pass=${NEXUS_PASSWORD}
[ -z "$user" ] && read -p "Nexus username: " user
[ -z "$pass" ] && read -p "Nexus password: " -s pass
echo ''

./gradlew   -Prelease.useAutomaticVersion=true  \
            -Prelease.releaseVersion=${version} \
            -Prelease.newVersion=${nextVersion} \
            -Pversion=${version}                \
            -PnexusUsername=${user}             \
            -PnexusPassword=${pass}             \
            -Ppush                              \
            clean                               \
            readme                              \
            release                             \
            uploadArchives                      \
            uberjar

echo ''
echo '/**********************/'
echo ''

# FIXME: In test repo, this can't checkout 'gh-pages' -- no error provided
#./gradlew   uploadDocs
echo TODO: upload javadocs to gh-pages with:
echo ./gradlew uploadDocs

# FIXME: hub is no longer able to find tagged releases for some reason.
#hub release edit -m '' v_${version} -a build/logback-android-${version}.jar
echo TODO: attach uber jar to release at:
echo https://github.com/tony19/logback-android/releases/tag/v_${version%*-SNAPSHOT}
