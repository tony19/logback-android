#!/bin/sh

##
# This script will add logback jars to your classpath.
##

LB_HOME=/SET/THIS/PARAMETER/TO/THE/DIRECTORY/WHERE/YOU/INSTALLED/LOGBACK

CLASSPATH="${CLASSPATH}:${LB_HOME}/logback-android-classic-${project.version}.jar"
CLASSPATH="${CLASSPATH}:${LB_HOME}/logback-android-core-${project.version}.jar"
CLASSPATH="${CLASSPATH}:${LB_HOME}/logback-examples/logback-android-examples-${project.version}.jar"
CLASSPATH="${CLASSPATH}:${LB_HOME}/logback-examples/lib/slf4j-api-${slf4j.version}.jar"

export CLASSPATH

echo $CLASSPATH
