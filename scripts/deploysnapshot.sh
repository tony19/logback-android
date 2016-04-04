#!/usr/bin/env bash
. gradle.properties
mvn deploy -DlogbackAndroidVersion=$version -Dslf4jVersion=$slf4jVersion
