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

read_outcome() {
  local step="$1" outcome error errors
  # Not a fixed path: agents run jobs concurrently and would share one.
  errors=$(mktemp)
  # Retried because a single API blip would otherwise announce a deploy that
  # worked as a failure, in a channel people read.
  for attempt in 1 2 3
  do
    if outcome=$(buildkite-agent step get "outcome" --step "$step" 2>"$errors") &&
       [[ -n "$outcome" ]]
    then
      echo "$outcome"
      rm -f "$errors"
      return 0
    fi
    if [[ "$attempt" == 3 ]]
    then
      # Said out loud: without it the log shows only "unreadable", and a
      # mistyped step key looks identical to a failed deploy.
      error=$(cat "$errors" 2>/dev/null)
      echo "could not read the outcome of '$step': ${error:-no error reported}" >&2
      break
    fi
    sleep 2
  done
  rm -f "$errors"
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
#
# Retried, then allowed to fail. The plugin tolerates failing to create a
# Deployment, so there may be nothing to post against, and on the stage pipeline
# this step sits before the wait that the prod trigger follows. Keeping the
# record must not stop the deploy being promoted, which is the same rule the
# plugin applies to its own pre-command hook.
buildkite-agent pipeline upload <<YAML
steps:
  - label: "Deployment $STATUS ($ENVIRONMENT)"
    depends_on: $DEPENDS
    allow_dependency_failure: true
    soft_fail: true
    retry:
      automatic:
        limit: 2
    plugins:
      - wellcomecollection/github-deployments#v0.4.0:
          assume_role: "arn:aws:iam::756629837203:role/catalogue-ci"
          environment: "$ENVIRONMENT"
          status: "$STATUS"
          environment_url: "$ENVIRONMENT_URL"
    agents:
      queue: nano
YAML
