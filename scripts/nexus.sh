#!/usr/bin/env bash -e

# If the VERSION_NAME in gradle.properties ends with -SNAPSHOT,
# the following gradle task will deploy the build to
# https://oss.sonatype.org/content/repositories/snapshots/com/github/tony19/named-regexp/.
# Otherwise, it goes to Nexus Staging. For now, you have to login
# to Nexus Repository Manager (https://oss.sonatype.org/) to release/drop.
#
# Make sure local.properties contains the following variables defined:
#
# signing.keyId=47C579C5
# signing.password=
# signing.key=<export private key to base64>
# ossrhUsername=<Sonatype JIRA username>
# ossrhPassword=<Sonatype JIRA password
# sonatypeStagingProfileId=b2413418ab44f

./gradlew logback-android:publishToSonatype logback-android:closeAndReleaseSonatypeStagingRepository --no-configuration-cache
