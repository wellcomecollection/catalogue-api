#!/usr/bin/env bash

set -o errexit
set -o nounset

PROJECT_NAME="$1"
PROJECT_DIRECTORY=$(./.buildkite/scripts/get_sbt_project_directory.sh "$PROJECT_NAME")

echo "*** Running sbt tests"

if [[ -f "$PROJECT_DIRECTORY/docker-compose.yml" ]]
then
  ./builds/run_sbt_task_in_docker.sh "project $PROJECT_NAME" "dockerComposeTest"
else
  ./builds/run_sbt_task_in_docker.sh "project $PROJECT_NAME" "test"
fi
