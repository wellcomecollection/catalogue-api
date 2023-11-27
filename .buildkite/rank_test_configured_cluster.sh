#!/usr/bin/env bash

set -o errexit
set -o nounset

ELASTIC_CONFIG_FILE="${BUILDKITE_BUILD_CHECKOUT_PATH}/common/search/src/main/scala/weco/api/search/models/ElasticConfig.scala"
CONFIGURED_PIPELINE_DATE=$(sed -nr 's/val pipelineDate = "(.+)"/\1/p' ${ELASTIC_CONFIG_FILE} | tr -d ' ')

buildkite-agent pipeline upload << EOF
  - label: "Rank: test query against $CONFIGURED_PIPELINE_DATE cluster ({{matrix}})"
    matrix:
      - "Works"
      - "Images"
    plugins:
      - wellcomecollection/aws-assume-role#v0.2.2:
          role: "arn:aws:iam::756629837203:role/catalogue-ci"
      - ecr#v2.7.0:
          login: true
      - docker#v5.8.0:
          image: 756629837203.dkr.ecr.eu-west-1.amazonaws.com/weco/rank:latest
          command:
            - "test"
            - "--content-type={{matrix}}"
            - "--pipeline-date=${CONFIGURED_PIPELINE_DATE}"
            - "--query=/resources/{{matrix}}Query.json"
          mount-checkout: false
          volumes:
            - "./search/src/main/resources:/resources"
          always-pull: true
          propagate-environment: true
          propagate-aws-auth-tokens: true
          shell: false
EOF
