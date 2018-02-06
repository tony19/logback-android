#!/usr/bin/env bash

user=${NEXUS_USERNAME}
pass=${NEXUS_PASSWORD}
[ -z "$user" ] && read -p "Nexus username: " user
[ -z "$pass" ] && read -p "Nexus password: " -s pass
echo ''

./gradlew uploadArchives -x test -x build -PNEXUS_USERNAME=${user} -PNEXUS_PASSWORD=${pass}
