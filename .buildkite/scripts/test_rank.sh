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
# TODO: This is a short-term fix to unbreak the build, find a better
# way to do this.
ROOT=$(git rev-parse --show-toplevel)
cp "$ROOT/search/src/test/resources/WorksMultiMatcherQuery.json" "$ROOT/rank/queries/WorksMultiMatcherQuery.json"
cp "$ROOT/search/src/test/resources/ImagesMultiMatcherQuery.json" "$ROOT/rank/queries/ImagesMultiMatcherQuery.json"

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
docker run \
    -v $HOME/.aws:/root/.aws \
    -v $(pwd):/catalogue-api \
    --workdir /catalogue-api/rank \
    public.ecr.aws/docker/library/node:14-slim \
    yarn test \
        --queryEnv=$QUERY_ENV \
        --cluster=pipeline \
        --index="$WORKS_INDEX" \
        --testId=alternative-spellings \
        --testId=precision \
        --testId=recall

# run images tests
IMAGES_INDEX=$(curl -s "${URL}" | jq -r .imagesIndex)
docker run \
    -v $HOME/.aws:/root/.aws \
    -v $(pwd):/catalogue-api \
    --workdir /catalogue-api/rank \
    public.ecr.aws/docker/library/node:14-slim \
    yarn test \
        --queryEnv=$QUERY_ENV \
        --cluster=pipeline \
        --index="$IMAGES_INDEX" \
        --testId=alternative-spellings \
        --testId=precision \
        --testId=recall
