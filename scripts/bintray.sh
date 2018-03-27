#!/usr/bin/env bash -ex

function prop {
    grep "${1}" local.properties|cut -d'=' -f2
}

bintray_user=$(prop BINTRAY_USER)
bintray_key=$(prop BINTRAY_KEY)
[ -z "$bintray_user" ] && read -p "Bintray username: " bintray_user
[ -z "$bintray_key" ] && read -p "Bintray API key: " bintray_key
echo ''

./gradlew   -PBINTRAY_USER=${bintray_user}      \
            -PBINTRAY_KEY=${bintray_key}        \
            bintrayUpload
