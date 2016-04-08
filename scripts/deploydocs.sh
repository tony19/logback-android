#!/usr/bin/env bash

# Generate javadoc for specified version...
if [ -n "$1" ]; then
    ./gradlew uploadDocs -Pversion="$1"
    exit 0
fi

# ...or prompt for the version
. gradle.properties

ver=${version%*-SNAPSHOT}
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
        ./gradlew uploadDocs -Pversion=${v}
        exit 0
    fi
done

echo aborting
