#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# install packages
docker run \
    -v $(pwd):/catalogue-api \
    --workdir /catalogue-api/rank \
    public.ecr.aws/docker/library/node:14-slim \
    yarn

# Copy candidate queries into place.
docker run \
    --volume $(pwd):/catalogue-api \
    --workdir /catalogue-api/rank \
    public.ecr.aws/docker/library/node:14-slim \
    yarn getQueries

case $QUERY_ENV in
    candidate | staging)
        SUBDOMAIN="api-stage"
        ;;
    production)
        SUBDOMAIN="api"
        ;;
    *)
        echo "Invalid QUERY_ENV: $QUERY_ENV"
        exit 1
        ;;
esac

URL="https://${SUBDOMAIN}.wellcomecollection.org/catalogue/v2/_elasticConfig"

# run works tests
WORKS_INDEX=$(curl -s "${URL}" | jq -r .worksIndex)
IMAGES_INDEX=$(curl -s "${URL}" | jq -r .imagesIndex)

for index in "$WORKS_INDEX" "$IMAGES_INDEX"
do
  docker run \
      --env AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
      --env AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
      --env AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN \
      --volume $HOME/.aws:/root/.aws \
      --volume $(pwd):/catalogue-api \
      --workdir /catalogue-api/rank \
      public.ecr.aws/docker/library/node:14-slim \
      yarn test \
          --queryEnv=$QUERY_ENV \
          --cluster=pipeline \
          --index="$index" \
          --testId=alternative-spellings \
          --testId=precision \
          --testId=recall
done
