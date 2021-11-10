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

# run tests
docker run \
    -v $(pwd):/catalogue-api \
    --workdir /catalogue-api/rank \
    --env ES_RANK_USER=$ES_RANK_USER \
    --env ES_RANK_PASSWORD=$ES_RANK_PASSWORD \
    --env ES_RANK_CLOUD_ID=$ES_RANK_CLOUD_ID \
    760097843905.dkr.ecr.eu-west-1.amazonaws.com/node:14-alpine \
    yarn test --queryEnv=production
