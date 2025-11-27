"""
Publish a new SnapshotJob to SNS.
"""

import os

import attr
import boto3
import httpx

from wellcome_aws_utils import log_on_error, publish_sns_message

from snapshot_job import get_snapshot_jobs


def get_current_api_elasticConfig():
    response = httpx.get(
        "https://api.wellcomecollection.org/catalogue/v2/_elasticConfig"
    )
    response_data = response.json()
    return {
        "works": response_data["worksIndex"],
        "images": response_data["imagesIndex"],
        "pipelineDate": response_data["pipelineDate"],
    }


@log_on_error
def main(event=None, _ctxt=None, sns_client=None, elastic_config=None):
    print(os.environ)
    sns_client = sns_client or boto3.client("sns")
    elastic_config = elastic_config or get_current_api_elasticConfig()

    snapshot_jobs = get_snapshot_jobs(elastic_config)
    topic_arn = os.getenv("TOPIC_ARN")
    for job in snapshot_jobs:
        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=attr.asdict(job),
            subject="source: snapshot_scheduler.main",
        )
