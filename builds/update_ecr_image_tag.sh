#!/usr/bin/env bash
<<EOF
Retag an image in ECR.

This script is mirrored in our other Scala repos.

== How we use ECR tags ==

We have three types of tag:

    - Every image has a tag starting ref, e.g. 'ref.f37d2c5'.
      This is the Git commit has that was used to build a given image.
      This helps us match an image to its source code.

    - The 'latest' tag points to the last image that was published to a
      repository. It helps us know what the newest version of our code is.

    - The env tags point to the image being used in a particular environment.
      e.g. 'env.stage', 'env.prod' would be the images used in our staging
      and prod environment, respectively.

      These are the tags used in our ECS task definitions.

This script updates the env tags of one or more repositories to point at
whatever 'latest' is, or at the tag named by LATEST_TAG if set. Deploys set
LATEST_TAG to the build's own ref tag so an environment runs the commit the
build was for, even if newer images have been published since.

== Worked example ==

Suppose you had the following set of images:

      image1      <-- ref.e6b6c49
      image2      <-- ref.ac3ec0a, env.prod
      image3      <-- ref.6b6213a, latest

If you ran this script to update the env.prod tag, it would be moved to
the image tagged 'latest':

      image1      <-- ref.e6b6c49
      image2      <-- ref.ac3ec0a
      image3      <-- ref.6b6213a, latest, env.prod

If CI then publishes a new image, this will become latest, but the image
pointed at by 'env.prod' will be unchanged:

      image1      <-- ref.e6b6c49
      image2      <-- ref.ac3ec0a
      image3      <-- ref.6b6213a, env.prod
      image4      <-- ref.8656d3f, latest

== Usage examples ==

    ENV_TAG="env.staging" update_ecr_image_tag.sh bag_replicator

        This will update the 'env.staging' tag in the bag_replicator ECR
        repository to point to the current value of 'latest'.

    ENV_TAG="env.prod" update_ecr_image_tag.sh id_minter transformer_mets

        This will update the 'env.staging' tag in the id_minter and
        transformer_mets ECR repositories to point to the current value of 'latest'.

        More generally you can supply an arbitrary number of ECR repos as
        additional arguments, and they will all be retagged.

    ENV_TAG="env.prod" LATEST_TAG="ref.8656d3f" update_ecr_image_tag.sh search

        This will update the 'env.prod' tag in the search ECR repository to
        point to the image tagged 'ref.8656d3f', failing if there is none.

EOF

set -o errexit
set -o nounset

LATEST_TAG=${LATEST_TAG:-latest}

for repositoryName in "$@"
do
  # These commands are based on an AWS ECR guide:
  # https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-retag.html
  echo "Updating the $ENV_TAG tag in $repositoryName to $LATEST_TAG"
  LATEST_MANIFEST=$(
    aws ecr batch-get-image \
      --repository-name "$repositoryName" \
      --image-ids imageTag="$LATEST_TAG" --output json \
      | jq --raw-output --join-output '.images[0].imageManifest'
  )

  if [[ "$LATEST_MANIFEST" == "null" ]]
  then
    echo "No image tagged $LATEST_TAG in $repositoryName" >&2
    exit 1
  fi

  TAGGED_MANIFEST=$(
    aws ecr batch-get-image \
      --repository-name "$repositoryName" \
      --image-ids imageTag="$ENV_TAG" --output json \
      | jq --raw-output --join-output '.images[0].imageManifest'
  )

  if [[ "$LATEST_MANIFEST" != "$TAGGED_MANIFEST" ]]
  then
    aws ecr put-image \
      --repository-name "$repositoryName" \
      --image-tag "$ENV_TAG" \
      --image-manifest "$LATEST_MANIFEST" >/dev/null
  else
    echo "Tag for $ENV_TAG is already up-to-date, skipping"
  fi
done
