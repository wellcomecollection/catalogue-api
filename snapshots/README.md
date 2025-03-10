# snapshots

Services for creating, recording and reporting on Catalogue API snapshots.

## Overview

Contains:

- `snapshot_scheduler`: a lambda triggered by CloudWatch, publishes messages to SNS describing required snapshots.
- `snapshot_generator`: an ECS service which polls SQS, produces a snapshot of documents from the Catalogue ES indices using their display model.
- `snapshot_recorder`: a lambda triggered by SNS, recording metadata in a reporting cluster Elasticsearch index.
- `snapshot_reporter`: a lambda triggered by CloudWatch, reports on daily snapshots in team Slack, will provide notification on failure.

## Architecture

![Architecture diagram for catalogue snapshots](architecture.png)

## Deployment

### Steps

The snapshot_generator is deployed alongside the catalogue API using Buildkite.

New Lambda deployment packages are published to S3 by Buildkite; you need to run a `terraform apply` to use the new versions.

## Running locally

To run the snapshot generator locally:

SNAPSHOT_QUERY='{"term": {"type": "Visible"}}'
SNAPSHOT_BUCKET_NAME="wellcomecollection-data-public-delta"
SNAPSHOT_BUCKET_KEY="catalogue_dev/v2/works.json.gz"
SNAPSHOT_INDEX="works-indexed-\*"
PIPELINE_DATE="2025-03-06"
AWS_PROFILE=catalogue-developer
AWS_REGION=eu-west-1
