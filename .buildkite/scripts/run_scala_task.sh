#!/usr/bin/env bash

set -o errexit
set -o nounset

ECR_REGISTRY="760097843905.dkr.ecr.eu-west-1.amazonaws.com"

parse_args() {
  if (( $# == 1 ))
  then
    PROJECT_NAME="$1"
    PROJECT_DIRECTORY="$1"
    PROJECT_TYPE="app"
  elif (( $# == 2 )) && [[ "$2" == "--is-library" ]]
  then
    PROJECT_NAME="$1"
    PROJECT_DIRECTORY="$1"
    PROJECT_TYPE="library"
  elif (( $# == 2))
  then
    PROJECT_NAME="$1"
    PROJECT_DIRECTORY="$2"
    PROJECT_TYPE="app"
  else
    echo "Usage: publish_sbt_app.sh <PROJECT> [<PROJECT_DIRECTORY> | --is-library]" >&2
    exit 1
  fi
}

run_sbt_tests() {
  echo "*** Running sbt tests"
  if [[ -f "$PROJECT_DIRECTORY/docker-compose.yml" ]]
  then
    ./builds/run_sbt_tests_with_docker_compose.sh "$PROJECT_NAME"
  else
    ./builds/run_sbt_tests.sh "$PROJECT_NAME"
  fi
}

build_image() {
  if [[ "$PROJECT_TYPE" == "library" ]]
  then
    echo "*** Skipping building a Docker image because this is a library"
    exit 0
  fi

  echo "*** Building a Docker image"
  ./builds/build_sbt_image.sh "$PROJECT_NAME" "$PROJECT_DIRECTORY" "$IMAGE_TAG"
}

publish_image_to_ecr() {
  if [[ "${BUILDKITE_BRANCH:-}" != "main" ]]
  then
    echo "*** Skipping publishing to ECR because we're not on main"
    exit 0
  fi

  echo "*** Publishing Docker image to ECR"

  eval $(aws ecr get-login --no-include-email)

  docker tag "$PROJECT_NAME:$IMAGE_TAG" "$ECR_REGISTRY/$PROJECT_NAME:$IMAGE_TAG"
  docker push "$ECR_REGISTRY/$PROJECT_NAME:$IMAGE_TAG"

  docker tag "$PROJECT_NAME:$IMAGE_TAG" "$ECR_REGISTRY/$PROJECT_NAME:latest"
  docker push "$ECR_REGISTRY/$PROJECT_NAME:latest"
}

parse_args "$@"
run_sbt_tests

IMAGE_TAG=$(git rev-parse HEAD)

build_image
publish_image_to_ecr
