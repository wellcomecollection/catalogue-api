#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

ECR_REGISTRY="760097843905.dkr.ecr.eu-west-1.amazonaws.com"
WECO_DEPLOY_IMAGE="wellcome/weco-deploy:5.6.11"

ROOT=$(git rev-parse --show-toplevel)

IMAGE_ID="$1"

# TODO: This is working out the weco-project project ID, which we could
# derive programatically from .wellcome_project.
case "$IMAGE_ID" in
  "search" | "items" | "snapshot_generator")
    PROJECT_ID="catalogue_api"
    ;;

  "requests")
    PROJECT_ID="requests_api"
    ;;
esac

docker run --tty --rm \
  --env AWS_PROFILE \
  --volume ~/.aws:/root/.aws \
  --volume /var/run/docker.sock:/var/run/docker.sock \
  --volume "${DOCKER_CONFIG:-$HOME/.docker}:/root/.docker" \
  --volume "$ROOT:$ROOT" \
  --workdir "$ROOT" \
  "$ECR_REGISTRY/$WECO_DEPLOY_IMAGE" \
    --project-id="$PROJECT_ID" \
    --verbose \
    publish \
    --image-id="$IMAGE_ID"
