#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

PROJECT="$1"
PROJECT_DIRECTORY="$2"
IMAGE_TAG="$3"

ROOT=$(git rev-parse --show-toplevel)
BUILDS_DIR="$ROOT/builds"

$BUILDS_DIR/run_sbt_task_in_docker.sh "project $PROJECT" ";stage"

docker build \
  --file "$PROJECT_DIRECTORY/Dockerfile" \
  --tag "$PROJECT:$IMAGE_TAG" \
  "$PROJECT_DIRECTORY"
