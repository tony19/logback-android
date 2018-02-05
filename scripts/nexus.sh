#!/usr/bin/env bash -e

user=${NEXUS_USERNAME}
pass=${NEXUS_PASSWORD}
[ -z "$user" ] && read -p "Nexus username: " user
[ -z "$pass" ] && read -p "Nexus password: " -s pass
echo ''

./gradlew closeAndReleaseRepository -PnexusUsername=$user -PnexusPassword=$pass
