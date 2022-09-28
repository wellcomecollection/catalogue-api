#!/usr/local/bin/bash

set -o errexit
set -o nounset

# Create a .env file if one doesn't already exist. The contents of an existing
# file will be unchanged
touch .env

# Read the existing environment variables from the .env file and store them as
# an associative array
echo "Reading environment variables from existing .env file"
declare -A environmentVariables
while IFS='=' read -r key value; do
  environmentVariables[$key]="$value"
done < .env

# Loop through a list of environment variable names, fetch the values from
# secretsmanager using the AWS CLI, and add them to the associative array
environmentVariableKeys=(
    "ES_RANK_CLOUD_ID"
    "ES_RANK_PASSWORD"
    "ES_RANK_USER"
    "ES_REPORTING_CLOUD_ID"
    "ES_REPORTING_PASSWORD"
    "ES_REPORTING_USER"
)

for key in ${environmentVariableKeys[@]}; do
    echo "Fetching $key from secretsmanager"
    environmentVariables[$key]=$(aws secretsmanager get-secret-value \
        --secret-id elasticsearch/rank/$key \
        --query SecretString \
        --output text
    )
done

# the associative array is then converted to a list of key=value pairs and
# written to a .env file, sorted by key
echo "Writing environment variables back to .env"
rm -f .env
touch .env
for key in $(printf '%s\n' "${!environmentVariables[@]}" | sort); do
    echo "$key=${environmentVariables[$key]}" >> .env
done
