#!/bin/bash

# mvn install:install-file
#  -Dfile=<path-to-file>
#  -DgroupId=<group-id>
#  -DartifactId=<artifact-id>
#  -Dversion=<version>
#  -Dpackaging=<packaging>
#  -DgeneratePom=true
#
#Where: <path-to-file>  the path to the file to load
#       <group-id>      the group that the file should be registered under
#       <artifact-id>   the artifact name for the file
#       <version>       the version of the file
#       <packaging>     the packaging of the file e.g. jar
#

mvn install:install-file \
  -Dfile=../ant/libs/apktool-min-android-1.4.3-1.jar \
  -DgroupId=brut.androlib.res \
  -DartifactId=decoder \
  -Dversion=1.4.3-1 \
  -Dpackaging=jar \
  -DgeneratePom=true
  
