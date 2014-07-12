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
# For a dryrun, enter:
#   $ ./release.sh dryrun
#

# Release version "x.x.x-N"
#
# where 
#	x.x.x is the version of logback on which logback-android is based
# and 
#	N is the integral release number of logback-android.
#
version=$(mvn help:evaluate -Dexpression=project.version | grep '^[^[]')
version=${version%-SNAPSHOT}
outf=logback-android-${version}.jar
outdir=$PWD/target
readme=$PWD/README.md
if [ "x$1" == "xdryrun" ]; then
  echo "[dryrun] just a test!"
  dryrun=true
fi
echo "Starting release process for logback-android ${version}..."

#
# Build the JAR and print its MD5. The last line uses GNU sed (gsed)
# to update the README with the current release version.
#
if [ ${dryrun} ]; then
  echo '[dryrun]'
  dryrunflag=-DdryRun=true
fi

mvn release:clean || exit 1
mvn -Dtag=v_${version} $dryrunflag release:prepare || exit 1
mvn -Dtag=v_${version} $dryrunflag release:perform || exit 1

mvn versions:set -DnewVersion=${version}
mvn clean install -DskipTests
mvn -f pom-uber.xml package -DskipTests -Dmy.project.version=${version}

md5 ${outdir}/${outf} && \
echo "Updating README.md..." && \
gsed -i -e "s/logback-android-[^j]*\.jar/${outf}/" \
-e "s/[0-9]\+\.[0-9]\+\.[0-9]\+-[0-9]\+/${version}/" \
-e "s/\(logback-android.*SHA1\:\).*/\1 \`$(openssl dgst -sha1 ${outdir}/${outf} | awk '{print $2}')\`)/" ${readme}

if [ ! ${dryrun} ]; then
git add ${readme}
git commit -m "Update README for release ${version}"
else
echo '[dryrun] skip commit README...'
fi

echo "Generating javadoc..."
mvn javadoc:javadoc

# Update the web pages
git clone -b gh-pages https://github.com/tony19/logback-android.git gh-pages
cd gh-pages
rm -rf doc/${version}
mv ${outdir}/site/apidocs doc/${version}

echo "Updating index.html..."
gsed -i -e "s/logback-android-[^j]*\.jar/${outf}/g" \
-e "s/[0-9]\+\.[0-9]\+\.[0-9]\+-[0-9]\+/${version}/g" index.html

if [ ! ${dryrun} ]; then
git add index.html
git add doc/${version}

# checkin changes to the web pages
git commit -m "release ${version}"

else
echo '[dryrun] skip commit gh-pages...'
fi

if [ ! ${dryrun} ]; then
echo Done. Push changes to GitHub!!
else
echo Done...just a dryrun!!
fi
