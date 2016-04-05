#!/usr/bin/env bash
. gradle.properties

[[ "$TRAVIS" == true ]] && settings='--settings config/travisMavenSettings.xml' || settings=''

mvn deploy $settings                    \
    -Pdebug                             \
    -B                                  \
    -DskipTests=true                    \
    -DlogbackAndroidVersion=$version    \
    -Dslf4jVersion=$slf4jVersion
