#!/usr/bin/env bash

NEXUS_USERNAME=${NEXUS_USERNAME?} \
NEXUS_PASSWORD=${NEXUS_PASSWORD?} \
./gradlew uploadArchives -x test
