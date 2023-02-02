# -*- encoding: utf-8 -*-

import datetime
import json
import os
from unittest.mock import patch

import snapshot_scheduler

pytest_plugins = "catalogue_aws_fixtures"


def test_writes_messages_to_sqs(
    test_topic_arn, mock_sns_client, get_test_topic_messages
):
    public_bucket_name = "public-bukkit"
    public_object_key_prefix = "catalogue/v2"
    test_pipeline_date = "2022-02-22"
    test_bulk_size = 1000

    patched_os_environ = {
        "TOPIC_ARN": test_topic_arn,
        "PUBLIC_BUCKET_NAME": public_bucket_name,
        "PUBLIC_OBJECT_KEY_PREFIX": public_object_key_prefix,
        "ES_BULK_SIZE": str(test_bulk_size)
    }
    test_indices = {
        "works": f"works-index-test-{test_pipeline_date}",
        "images": f"images-index-test-{test_pipeline_date}"
    }

    with patch.dict(os.environ, patched_os_environ, clear=True):
        snapshot_scheduler.main(sns_client=mock_sns_client, indices=test_indices)

    messages = list(get_test_topic_messages())
    assert len(messages) == 2

    assert all(m["s3Location"]["bucket"] == public_bucket_name for m in messages)
    assert all(m["pipelineDate"] == test_pipeline_date for m in messages)
    assert all(m["bulkSize"] == test_bulk_size for m in messages)

    def took_less_than_five_seconds(requested_at_str):
        requested_at = datetime.datetime.strptime(requested_at_str, "%Y-%m-%dT%H:%M:%SZ")
        return (datetime.datetime.now() - requested_at).total_seconds() < 5

    assert all(took_less_than_five_seconds(m["requestedAt"]) for m in messages)

    works_snapshot_job = next(m for m in messages if m["index"]["name"] == test_indices["works"])
    images_snapshot_job = next(m for m in messages if m["index"]["name"] == test_indices["images"])

    assert works_snapshot_job["s3Location"]["key"] == f"{public_object_key_prefix}/works.json.gz"
    assert images_snapshot_job["s3Location"]["key"] == f"{public_object_key_prefix}/images.json.gz"

    assert json.loads(works_snapshot_job["query"]) == {"term": {"type": "Visible"}}
    assert images_snapshot_job["query"] is None

