#!/bin/sh -e

. ./gradle.properties

token=${CIRCLE_TOKEN?}
username=tony19-sandbox
project=logback-test-app

curl -X POST  --header "Content-Type: application/json" -d '{
    "build_parameters": {
      "LOGBACK_ANDROID_VERSION": "'${VERSION_NAME}'",
      "SLF4J_VERSION": "'$slf4jVersion'",
      "CIRCLE_BRANCH": "'$CIRCLE_BRANCH'",
      "CIRCLE_TAG": "'$CIRCLE_TAG'",
      "CIRCLE_SHA1": "'$CIRCLE_SHA1'",
      "CIRCLE_REPOSITORY_URL": "'$CIRCLE_REPOSITORY_URL'",
      "CIRCLE_COMPARE_URL": "'$CIRCLE_COMPARE_URL'",
      "CIRCLE_BUILD_URL": "'$CIRCLE_BUILD_URL'",
      "CIRCLE_BUILD_NUM": "'$CIRCLE_BUILD_NUM'",
      "CI_PULL_REQUESTS": "'$CI_PULL_REQUESTS'",
      "CI_PULL_REQUEST": "'$CI_PULL_REQUEST'",
      "CIRCLE_USERNAME": "'$CIRCLE_USERNAME'",
      "CIRCLE_PR_USERNAME": "'$CIRCLE_PR_USERNAME'",
      "CIRCLE_PR_REPONAME": "'$CIRCLE_PR_REPONAME'",
      "CIRCLE_PR_NUMBER": "'$CIRCLE_PR_NUMBER'"
    }
  }' https://circleci.com/api/v1/project/${username}/${project}?circle-token=${token}
