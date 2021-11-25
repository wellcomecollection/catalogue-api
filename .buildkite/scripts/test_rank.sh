#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# use aws cli to fetch secrets from aws secrets manager
ES_RANK_USER=$(aws secretsmanager get-secret-value \
        --secret-id elasticsearch/rank/buildkite_user \
        --query SecretString \
        --output text
)
ES_RANK_PASSWORD=$(aws secretsmanager get-secret-value \
        --secret-id elasticsearch/rank/buildkite_password \
        --query SecretString \
        --output text
)
ES_RANK_CLOUD_ID=$(aws secretsmanager get-secret-value \
        --secret-id elasticsearch/rank/cloud_id \
        --query SecretString \
        --output text
)
echo "ES_RANK_USER: $ES_RANK_USER"

# install packages
docker run \
    -v $(pwd):/catalogue-api \
    --workdir /catalogue-api/rank \
    760097843905.dkr.ecr.eu-west-1.amazonaws.com/node:14-alpine \
    yarn

# Copy candidate queries into place.
# TODO: This is a short-term fix to unbreak the build, find a better
# way to do this.
ROOT=$(git rev-parse --show-toplevel)
cp "$ROOT/search/src/test/resources/WorksMultiMatcherQuery.json" "$ROOT/rank/public/WorksMultiMatcherQuery.json"
cp "$ROOT/search/src/test/resources/ImagesMultiMatcherQuery.json" "$ROOT/rank/public/ImagesMultiMatcherQuery.json"

# run tests
docker run \
    -v $(pwd):/catalogue-api \
    --workdir /catalogue-api/rank \
    --env ES_RANK_USER=$ES_RANK_USER \
    --env ES_RANK_PASSWORD=$ES_RANK_PASSWORD \
    --env ES_RANK_CLOUD_ID=$ES_RANK_CLOUD_ID \
    760097843905.dkr.ecr.eu-west-1.amazonaws.com/node:14-alpine \
    yarn test --queryEnv=production
