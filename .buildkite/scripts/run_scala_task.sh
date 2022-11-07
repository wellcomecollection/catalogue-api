#!/usr/bin/env bash
<<EOF
Run a Scala task in Buildkite.  This includes:

  - running tests
  - building a Docker image (if applicable)
  - publishing the Docker image to ECR (if on main)

The script will try to be intelligent about what to do, e.g. deciding whether
to publish images or whether tests need docker-compose.

This script is mirrored in the catalogue-pipeline and storage-service repos.

== Usage examples ==

    run_scala_task.sh bag_verifier

        This will run the tests for the 'bag_verifier' app, which is
        in the 'bag_verifier' folder in the root of the repo.
        It will build the image on pull requests, and on pushes to main
        it will also publish the image to ECR.

    run_scala_task.sh snapshot_generator snapshots/snapshot_generator

        This runs the tests for the 'snapshot_generator' app, which is
        in the 'snapshots/snapshot_generator' folder.  It also builds and
        publishes Docker images on pull requests/pushes to main, respectively.

        This second argument is useful in larger repositories where not
        every app is in the top level.

    run_scala_task.sh display --is-library

        This runs the tests for the 'display' library, which is in the
        'display' folder in the root of the repo.  It does not build or
        publish Docker images, because we don't have them for libraries.

    run_scala_task.sh pipeline_storage common/pipeline_storage --is-library

        This runs the tests for the 'pipeline_storage' library, which is
        in the 'common/pipeline_storage' folder in the root of the repo.
        It doesn't build or publish Docker images.

        This second argument is useful in larger repositories where not
        every library is in the top level.

EOF

set -o errexit
set -o nounset

ECR_REGISTRY="760097843905.dkr.ecr.eu-west-1.amazonaws.com"

parse_args() {
  if (( $# == 1 ))
  then
    PROJECT_NAME="$1"
    PROJECT_DIRECTORY="$1"
    PROJECT_TYPE="app"
  elif (( $# == 3 )) && [[ "$3" == "--is-library" ]]
    PROJECT_NAME="$1"
    PROJECT_DIRECTORY="$2"
    PROJECT_TYPE="library"
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
