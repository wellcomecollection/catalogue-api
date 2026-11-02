#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

docker run --tty --rm \
	--volume "$ROOT:/repo" \
	--workdir /repo \
	"public.ecr.aws/hashicorp/terraform:light" fmt -recursive

./builds/run_sbt_task_in_docker.sh "scalafmt"

# Undo any formatting changes to the source_model code copied from
# the pipeline repo.  Ideally I'd have scalafmt ignore them, but I
# can't work out how to do that.
git checkout common/stacks/src/main/scala/weco/catalogue/source_model
git checkout common/stacks/src/test/scala/weco/catalogue/source_model

docker run --tty --rm \
	--volume "$ROOT:/repo" \
  --workdir /repo \
	"ghcr.io/astral-sh/ruff:0.16.1" \
  format --extend-exclude ".lambda_zips,.terraform,target,*.ipynb,*.md" .
