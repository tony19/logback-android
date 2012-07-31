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
if [ ! ${dryrun} ]; then
mvn release:clean || exit 1
mvn -Dtag=v_${version} release:prepare || exit 1
mvn -Dtag=v_${version} release:perform || exit 1
else
echo '[dryrun] skip mvn release...'
fi

mvn versions:set -DnewVersion=${version}
mvn clean install -DskipTests
mvn -f pom-uber.xml package -DskipTests -Dmy.project.version=${version}

md5 ${outdir}/${outf} && \
echo "Updating README.md..." && \
gsed -i -e "s/logback-android-[^j]*\.jar/${outf}/" \
-e "s/[0-9]\+\.[0-9]\+\.[0-9]\+-[0-9]\+/${version}/" \
-e "s/\(logback-android.*MD5\:\).*/\1 \`$(md5 ${outdir}/${outf} | awk '{print $4}')\`)/" ${readme}

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

insdiv="<div id=\"${version}\" class=\"release\">\n\
\t\t<div class=\"version\">${version}</div>\n\
\t\t<div class=\"reldate\">Released on $(date +'%m-%d-%Y')</div>\n\
\t\t<ul>\n\
\t\t\t<li>Sync with Logback <a href=\"http://logback.qos.ch/news.html\">${version%-*}</a></li>\n\
\t\t</ul>\n\
\t</div>"

# insert the release div if not already present (it shouldn't be present)
grep -q "id=\"${version}\" class=\"release\"" changelog.html
if [ $? -gt 0 ]; then
	echo "Updating changelog.html..."
	gsed -i -e "s@<div id=[\"\']notes[\"\']>@&\n\t${insdiv}@" changelog.html
fi

if [ ! ${dryrun} ]; then
git add index.html
git add doc/${version}
git add changelog.html

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
