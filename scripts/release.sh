#!/usr/bin/env bash -e
# Usage:
#  ./release.sh [TARGET]
#
# Options:
#   TARGET         "major", "minor", or "build" (default: "build")

. gradle.properties

function nextVersion() {
    target=$1
    ver=$2
    case "$target" in
        "major")
            baseVer=${ver%*.*}
            majorVer=${baseVer%*.*}
            nextMajor=$(($majorVer + 1))
            nextVer="${nextMajor}.0.0-SNAPSHOT"
            echo $nextVer
        ;;

        "minor")
            baseVer=${ver%*.*}
            minorVer=${baseVer##*.}
            nextMinor=$(($minorVer + 1))
            nextVer="${baseVer%*.*}.${nextMinor}.0-SNAPSHOT"
            echo $nextVer
        ;;

        "patch")
            baseVer=${ver%*}
            baseVer=${baseVer%*}
            patchVer=${baseVer##*.}
            nextPatch=$(($patchVer + 1))
            nextVer="${baseVer%*.*}.${nextPatch}-SNAPSHOT"
            echo $nextVer
        ;;
    esac
}

versionTarget=${1:-patch}
version=${VERSION_NAME%*-SNAPSHOT}
nextVersion=$(nextVersion "$versionTarget" "$version")

echo "Starting release for logback-android-${version} ..."

./gradlew   -Prelease.useAutomaticVersion=true  \
            -Prelease.releaseVersion=${version} \
            -Prelease.newVersion=${nextVersion} \
            -Pversion=${version}                \
            -PVERSION_NAME=${version}           \
            clean                               \
            release

# To deploy archives without git transactions (tagging, etc.),
# replace the `release` task above with `assembleRelease`.

echo -e "\n\n"

# FIXME: In test repo, this can't checkout 'gh-pages' -- no error provided
#./gradlew   uploadDocs
echo TODO: upload javadocs to gh-pages with:
echo scripts/deploydocs.sh ${version}

# FIXME: hub is no longer able to find tagged releases for some reason.
#hub release edit -m '' v_${version} -a build/logback-android-${version}.jar
echo TODO: attach AAR to release at:
echo https://github.com/tony19/logback-android/releases/tag/v_${version}
