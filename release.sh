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

# This script performs a logback-android release:
#  * sets the version (removes SNAPSHOT suffix from current version,
#    and bumps to the next SNAPSHOT version when release is finished)
#  * tags the source in GitHub (e.g., with "v_1.0.2")
#  * deploys the signed jars to Sonatype
#  * updates the README.md with the current version info
#  * updates GitHub pages with javadocs and current version info
#  * generates the javadoc and uploads it to GitHub pages
#
# To run this script, simply run:
#   $ ./release.sh
#

# Release version "x.x.x-N"
#
# where
#	x.x.x is the version of logback on which logback-android is based
# and
#	N is the integral release number of logback-android.
#
. gradle.properties
version=${baseVersion}-${buildVersion}
outf=logback-android-${version}.jar

echo "Updating README.md..."
./gradlew readme

echo "Starting release process for logback-android ${version}..."

# Deploy release to Sonatype
./gradlew clean build uploadArchives -Pver=${version}

echo "Create release version of uber-jar..."
./makejar.sh -r

echo "Generating javadoc..."
./gradlew javadocs

# Update the web pages
git clone -b gh-pages https://github.com/tony19/logback-android.git gh-pages
cd gh-pages
rm -rf doc/${version}
mv $PWD/build/docs/javadoc doc/${version}

echo "Updating index.html..."
gsed -i -e "s/logback-android-[^j]*\.jar/${outf}/g" \
-e "s/[0-9]\+\.[0-9]\+\.[0-9]\+-[0-9]\+/${version}/g" index.html

git add index.html
git add doc/${version}

# checkin changes to the web pages
git commit -m "release ${version}"

echo Done. Push changes to GitHub!!
