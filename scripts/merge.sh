#!/bin/sh
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

if [ ! $1 ]; then
	echo "Usage: $0 {version}"
	exit 1
fi

version=$1

echo "Fetching the latest logback code from Github..."
git remote add upstream https://github.com/qos-ch/logback.git
git fetch upstream

echo "Merging with logback ${version%-*}..."
git merge tags/${version%-*}
git rm -rf logback-access logback-site logback-examples

# Delete all tags from upstream that don't belong to logback-android
echo "Deleting all extraneous tags..."
for x in $(git tag -l)
do
	if [[ ! $x =~ [0-9]+\.[0-9]+\.[0-9]+-[0-9]+ ]]; then
		git tag -d $x
	fi
done
