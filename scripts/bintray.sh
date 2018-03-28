#!/usr/bin/env bash -e

. gradle.properties

function prop {
    grep "${1}" local.properties|cut -d'=' -f2
}

uploadArchives() {
    version=${1?}

    echo "Uploading logback-android-${version} to bintray ..."

    bintray_user=$(prop BINTRAY_USER)
    bintray_key=$(prop BINTRAY_KEY)
    [ -z "$bintray_user" ] && read -p "Bintray username: " bintray_user
    [ -z "$bintray_key" ] && read -p "Bintray API key: " bintray_key
    echo ''

    ./gradlew   -PBINTRAY_USER=${bintray_user}      \
                -PBINTRAY_KEY=${bintray_key}        \
                -PVERSION_NAME=${version}           \
                -Pversion=${version}                \
                bintrayUpload
}

# Generate javadoc for specified version...
if [ -n "$1" ]; then
    uploadArchives "$1"
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
    read -p "Upload archives to Bintray for ${v}? [y|N] " prompt
    if [ "$prompt" == "y" ]; then
        uploadArchives "${v}"
        exit 0
    fi
done

echo aborting
