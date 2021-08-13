#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

ECR_REGISTRY="760097843905.dkr.ecr.eu-west-1.amazonaws.com"

ROOT=$(git rev-parse --show-toplevel)

# Coursier cache location is platform-dependent
# https://get-coursier.io/docs/cache.html#default-location
LINUX_COURSIER_CACHE=".cache/coursier/v1"

if [[ $(uname) == "Darwin" ]]
then
  HOST_COURSIER_CACHE=~/Library/Caches/Coursier/v1
else
  HOST_COURSIER_CACHE=~/$LINUX_COURSIER_CACHE
fi

docker run --tty --rm \
	--volume "$ROOT:/repo" \
	--workdir /repo \
	"$ECR_REGISTRY/hashicorp/terraform:light" fmt -recursive

docker run --tty --rm \
  --volume ~/.sbt:/root/.sbt \
  --volume ~/.ivy2:/root/.ivy2 \
  --volume "$HOST_COURSIER_CACHE:/root/$LINUX_COURSIER_CACHE" \
	--volume "$ROOT:/repo" \
	"$ECR_REGISTRY/wellcome/scalafmt:edge"

# Undo any formatting changes to the source_model code copied from
# the pipeline repo.  Ideally I'd have scalafmt ignore them, but I
# can't work out how to do that.
git checkout common/stacks/src/main/scala/weco/catalogue/source_model
git checkout common/stacks/src/test/scala/weco/catalogue/source_model

docker run --tty --rm \
	--volume "$ROOT:/repo" \
	"$ECR_REGISTRY/wellcome/format_python:112"
