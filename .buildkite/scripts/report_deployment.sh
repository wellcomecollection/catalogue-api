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
# Usage: report_deployment.sh <environment> <deploy step key>...

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

# The key the plugin writes the id under. task is left at the plugin's default
# on the deploy steps, so it is the default here too.
METADATA_KEY="github_deployment:$ENVIRONMENT:deploy:weco:id"

read_outcome() {
  local step="$1" outcome
  # Retried because a single API blip would otherwise announce a deploy that
  # worked as a failure, in a channel people read.
  for _ in 1 2 3
  do
    if outcome=$(buildkite-agent step get "outcome" --step "$step" 2>/dev/null) &&
       [[ -n "$outcome" ]]
    then
      echo "$outcome"
      return 0
    fi
    sleep 2
  done
  echo "unreadable"
}

STATUS="success"

for step in "$@"
do
  OUTCOME=$(read_outcome "$step")
  echo "$step: $OUTCOME"

  # passed is the only outcome that is not a failure. The others are
  # hard_failed, soft_failed and errored, and errored covers a cancelled or
  # timed out job, which should not be announced as a deploy that worked.
  if [[ "$OUTCOME" != "passed" ]]
  then
    STATUS="failure"
  fi
done

# No id means the plugin never created a Deployment, which it tolerates so that
# announcing a deploy cannot stop one. There is nothing to post against, and
# that is the one case where posting nothing is right. Checked here so that the
# status step itself can stay fatal, and a GitHub or Secrets Manager failure
# still turns the build red rather than leaving a live Deployment in_progress.
if ! buildkite-agent meta-data exists "$METADATA_KEY"
then
  echo "No deployment was created for $ENVIRONMENT, so there is nothing to report"
  exit 0
fi

# The same dependencies this step has. An uploaded step is inserted after the
# job that uploads it, which would put it behind the wait the deploy steps sit in
# front of, and a failure before that wait stops everything after it. Naming the
# deploy steps rather than this one gives it the same exemption without making it
# depend on the step that uploads it, which would be self-referential.
DEPENDS=$(printf '"%s", ' "$@")
DEPENDS="[${DEPENDS%, }]"

echo "Reporting the $ENVIRONMENT deployment as $STATUS"

# environment_url is set here rather than on the deploy step because GitHub
# takes it from the status, not from the deployment.
buildkite-agent pipeline upload <<YAML
steps:
  - label: "Deployment $STATUS ($ENVIRONMENT)"
    depends_on: $DEPENDS
    allow_dependency_failure: true
    plugins:
      - wellcomecollection/github-deployments#v0.4.0:
          assume_role: "arn:aws:iam::756629837203:role/catalogue-ci"
          environment: "$ENVIRONMENT"
          status: "$STATUS"
          environment_url: "$ENVIRONMENT_URL"
    agents:
      queue: nano
YAML
