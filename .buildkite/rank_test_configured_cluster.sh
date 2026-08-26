#!/usr/bin/env bash

set -o errexit
set -o nounset

ELASTIC_CONFIG_FILE="${BUILDKITE_BUILD_CHECKOUT_PATH}/common/search/src/main/scala/weco/api/search/models/ElasticConfig.scala"

read_config_date() {
  sed -nr "s/^[[:space:]]*val $1 = \"([^\"]+)\".*\$/\1/p" "${ELASTIC_CONFIG_FILE}" | tr -d ' '
}

CONFIGURED_PIPELINE_DATE=$(read_config_date defaultPipelineDate)
WORKS_INDEX_DATE=$(read_config_date defaultWorksIndexDate)
IMAGES_INDEX_DATE=$(read_config_date defaultImagesIndexDate)

# Without this, a rename in ElasticConfig.scala leaves a date empty and rank is
# quietly asked to test '--pipeline-date=' instead of failing here.
if [[ -z "${CONFIGURED_PIPELINE_DATE}" ]] ||
   [[ -z "${WORKS_INDEX_DATE}" ]] ||
   [[ -z "${IMAGES_INDEX_DATE}" ]]; then
  echo "Could not read pipeline and index dates from ${ELASTIC_CONFIG_FILE}" >&2
  echo "  defaultPipelineDate:    '${CONFIGURED_PIPELINE_DATE}'" >&2
  echo "  defaultWorksIndexDate:  '${WORKS_INDEX_DATE}'" >&2
  echo "  defaultImagesIndexDate: '${IMAGES_INDEX_DATE}'" >&2
  exit 1
fi

# Index dates are tracked separately from the pipeline date, so each content type
# names its index explicitly. This is why the two steps aren't a matrix.
emit_step() {
  local content_type="$1"
  local index="$2"

  cat << EOF
  - label: "Rank: test ${content_type} query against ${CONFIGURED_PIPELINE_DATE} cluster"
    plugins:
      - wellcomecollection/aws-assume-role#v0.2.2:
          role: "arn:aws:iam::756629837203:role/catalogue-ci"
      - ecr#v2.7.0:
          login: true
      - docker#v5.8.0:
          image: 756629837203.dkr.ecr.eu-west-1.amazonaws.com/weco/rank:latest
          command:
            - "test"
            - "--content-type=${content_type}"
            - "--pipeline-date=${CONFIGURED_PIPELINE_DATE}"
            - "--index=${index}"
            - "--query=/resources/${content_type}Query.json"
          mount-checkout: false
          volumes:
            - "./search/src/main/resources:/resources"
          always-pull: true
          propagate-environment: true
          propagate-aws-auth-tokens: true
          shell: false
EOF
}

{
  emit_step "Works" "works-indexed-${WORKS_INDEX_DATE}"
  emit_step "Images" "images-indexed-${IMAGES_INDEX_DATE}"
} | buildkite-agent pipeline upload
