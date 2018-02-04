#!/usr/bin/env bash -e


uploadJavadoc() {
    version=${1?}
    oldbranch=$(git rev-parse --abbrev-ref HEAD)
    git checkout gh-pages
    cp -rf logback-android/build/docs/javadoc doc/${version}
    git add doc/${version}
    git commit -m ":books: Add javadoc for ${version}"
    #git push origin gh-pages
    git checkout ${oldbranch}
}

# Generate javadoc for specified version...
if [ -n "$1" ]; then
    ./gradlew generateReleaseJavadoc -Pversion="$1"
    uploadJavadoc "$1"
    exit 0
fi

# ...or prompt for the version
. gradle.properties

ver=${VERSION_NAME%*-SNAPSHOT}
baseVersion=${ver%*-*}
prevBuild=$((${ver##*-} - 1))
prevVersion="${baseVersion}-${prevBuild}"

versions="
${prevVersion}
${ver}
${version}
"

for v in ${versions}; do
    read -p "Release javadoc for ${v}? [y|N] " prompt
    if [ "$prompt" == "y" ]; then
        ./gradlew generateReleaseJavadoc -Pversion=${v}
        uploadJavadoc "${v}"
        exit 0
    fi
done

echo aborting
