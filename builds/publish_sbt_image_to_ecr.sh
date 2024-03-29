#!/usr/bin/env bash
<<EOF
Publish a Docker image to ECR.

This is meant for use with sbt-based images -- we use Docker Compose for
our Node apps.  The image will be published both with the given tag and
the floating tag 'latest'.

This script is mirrored in our other Scala repos.

== Usage ==

Pass the name of the sbt project as arg 1, and the image tag as arg 2, e.g.

    $ publish_sbt_image_to_ecr.sh file_indexer ref.19872ab
    $ publish_sbt_image_to_ecr.sh snapshot_generator ref.1761817
    $ publish_sbt_image_to_ecr.sh transformer_mets ref.9811987

EOF

set -o errexit
set -o nounset

if (( $# == 2))
then
  PROJECT_NAME="$1"
  IMAGE_TAG="$2"
else
  echo "Usage: publish_sbt_image_to_ecr.sh <PROJECT> <IMAGE_TAG>" >&2
  exit 1
fi

if [[ "$PROJECT_NAME" == "requests" ]]
then
  # identity
  ECR_REGISTRY="770700576653.dkr.ecr.eu-west-1.amazonaws.com"
else
  # catalogue
  ECR_REGISTRY="756629837203.dkr.ecr.eu-west-1.amazonaws.com"
fi

echo "*** Publishing Docker image to ECR"

aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY

IMAGE_REPOSITORY_PREFIX="$ECR_REGISTRY/uk.ac.wellcome"

docker tag "$PROJECT_NAME:$IMAGE_TAG" "$IMAGE_REPOSITORY_PREFIX/$PROJECT_NAME:$IMAGE_TAG"
docker push "$IMAGE_REPOSITORY_PREFIX/$PROJECT_NAME:$IMAGE_TAG"

docker tag "$PROJECT_NAME:$IMAGE_TAG" "$IMAGE_REPOSITORY_PREFIX/$PROJECT_NAME:latest"
docker push "$IMAGE_REPOSITORY_PREFIX/$PROJECT_NAME:latest"
