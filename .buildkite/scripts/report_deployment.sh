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

# An uploaded step is inserted after this job, which puts it behind the wait that
# the deploy steps sit in front of. A failure anywhere before that wait stops
# everything after it, which is the one case this script exists for, so the
# uploaded step depends on this one and inherits its exemption from the wait.
if [[ -z "${BUILDKITE_STEP_KEY:-}" ]]
then
  echo "Refusing to report: this step needs a key for the status step to depend on" >&2
  exit 1
fi

STATUS="success"

for step in "$@"
do
  # Never fatal. This script's whole job is to resolve the Deployment, so a step
  # key that no longer exists, or an agent API blip, has to read as a failure
  # rather than abort and leave the Deployment in_progress for good.
  OUTCOME=$(buildkite-agent step get "outcome" --step "$step" || echo "unreadable")
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
#
# soft_fail because this step only writes the record. The plugin tolerates
# failing to create a Deployment, so there may be none to post against, and a
# deploy that worked should not go red over the note kept about it.
buildkite-agent pipeline upload <<YAML
steps:
  - label: "Deployment $STATUS ($ENVIRONMENT)"
    depends_on: ["$BUILDKITE_STEP_KEY"]
    allow_dependency_failure: true
    soft_fail: true
    plugins:
      - wellcomecollection/github-deployments#v0.4.0:
          assume_role: "arn:aws:iam::756629837203:role/catalogue-ci"
          environment: "$ENVIRONMENT"
          status: "$STATUS"
          environment_url: "$ENVIRONMENT_URL"
    agents:
      queue: nano
YAML
