#!/usr/bin/env bash
# Completes the GitHub Deployment this build created, so the deploy tracker
# stops waiting on it.
#
# A step's plugin config is fixed when the pipeline is uploaded, so the status
# cannot depend on what the earlier steps did, and Buildkite offers no step-level
# condition that can read one: build.state is pipeline level only and meta-data
# is not available to `if` at all. A dynamic upload is what the docs point to
# instead, so this reads the outcomes and uploads a step carrying the status they
# add up to.
#
# Usage: report_deployment.sh <environment> <step key>...

set -o errexit
set -o nounset

ENVIRONMENT="$1"
shift

case "$ENVIRONMENT" in
  stage) ENVIRONMENT_URL="https://api-stage.wellcomecollection.org" ;;
  prod) ENVIRONMENT_URL="https://api.wellcomecollection.org" ;;
  *)
    echo "Refusing to report: unknown environment '$ENVIRONMENT'" >&2
    exit 1
    ;;
esac

if [[ $# -eq 0 ]]
then
  echo "Refusing to report: no steps to read an outcome from" >&2
  exit 1
fi

STATUS="success"

for step in "$@"
do
  OUTCOME=$(buildkite-agent step get "outcome" --step "$step")
  echo "$step: $OUTCOME"

  # passed is the only outcome that is not a failure. The others are
  # hard_failed, soft_failed and errored, and errored covers a cancelled or
  # timed out job, which should not be announced as a deploy that worked.
  if [[ "$OUTCOME" != "passed" ]]
  then
    STATUS="failure"
  fi
done

echo "Reporting the $ENVIRONMENT deployment as $STATUS"

# environment_url is set here rather than on the deploy step because GitHub
# takes it from the status, not from the deployment.
buildkite-agent pipeline upload <<YAML
steps:
  - label: "Deployment $STATUS ($ENVIRONMENT)"
    plugins:
      - wellcomecollection/github-deployments#v0.4.0:
          assume_role: "arn:aws:iam::756629837203:role/catalogue-ci"
          environment: "$ENVIRONMENT"
          status: "$STATUS"
          environment_url: "$ENVIRONMENT_URL"
    agents:
      queue: nano
YAML
