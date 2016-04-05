#!/bin/sh -e
##############################################################################
# Logback: the reliable, generic, fast and flexible logging framework.
# Copyright (C) 2011-2012, Anthony Trinh. All rights reserved.
# Copyright (C) 1999-2011, QOS.ch. All rights reserved.
#
# This program and the accompanying materials are dual-licensed under
# either the terms of the Eclipse Public License v1.0 as published by
# the Eclipse Foundation
#
#   or (per the licensee's choosing)
#
# under the terms of the GNU Lesser General Public License version 2.1
# as published by the Free Software Foundation.
##############################################################################

. gradle.properties
version=${baseVersion}-${buildVersion}

echo "Starting release process for logback-android ${version}..."

./gradlew -Pversion=${version}  \
            -Ppush              \
            clean               \
            readme              \
            release             \
            uploadArchives      \
            uploadDocs          \
            uberjar

hub release edit -d -m '' ${version} -a build/logback-android-${version}.jar
