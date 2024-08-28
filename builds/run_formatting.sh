#!/usr/bin/env bash

set -o errexit
set -o nounset

ECR_REGISTRY="760097843905.dkr.ecr.eu-west-1.amazonaws.com"

ROOT=$(git rev-parse --show-toplevel)

docker run --tty --rm \
	--volume "$ROOT:/repo" \
	--workdir /repo \
	"public.ecr.aws/hashicorp/terraform:light" fmt -recursive

./builds/run_sbt_task_in_docker.sh "scalafmt" AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID AWS_SECRET_KEY=$AWS_SECRET_KEY AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN

# Undo any formatting changes to the source_model code copied from
# the pipeline repo.  Ideally I'd have scalafmt ignore them, but I
# can't work out how to do that.
git checkout common/stacks/src/main/scala/weco/catalogue/source_model
git checkout common/stacks/src/test/scala/weco/catalogue/source_model

docker run --tty --rm \
	--volume "$ROOT:/repo" \
  --workdir /repo \
	"$ECR_REGISTRY/pyfound/black" \
  black --exclude ".lambda_zips/|.terraform/|target/" .
