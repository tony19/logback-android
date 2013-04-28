#!/bin/sh
#
# This script creates the uber jar with debug symbols. The tests are skipped.
# The jar will be stored in the "./target" directory:
#
#   ./target/logback-android-<version>.jar
#

set -e

mvn -P debug clean install -DskipTests=true
mvn -f pom-uber.xml package
