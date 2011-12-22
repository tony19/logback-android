#!/bin/sh
##############################################################################
# Logback: the reliable, generic, fast and flexible logging framework.
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

readme=../../README.md
cd build/ant || exit 1

#
# Release version "x.x.x-N"
#
# where 
#	x.x.x is the version of Logback on which Logback-Android is based
# and 
#	N is the integral release number of Logback-Android.
#
version=1.0.0-2

echo "Starting release process for Logback-Android ${version}..."

# prompt for keystore password (hide input)
echo "Enter keystore password to sign JAR: "
stty -echo
read password
stty echo

#
# Build the JAR and print its MD5. The last line uses GNU sed (gsed)
# to update the README with the current release version.
#
ant release -Dkey.store.password=${password} -Dversion=${version} && \
md5 bin/logback-android-${version}.jar && \
echo "Updating README.md" && \
gsed -i -e "s/logback-android-[^j]*\.jar/logback-android-${version}.jar/" \
-e "s/\*\*[0-9\.\-]*\*\*/\*\*${version}\*\*/" \
-e "s/\(logback-android.*MD5\:\).*\\)/\1 `md5 bin/logback-android-${version}.jar | awk '{print $4}'`\)/" \
${readme}

echo Done
