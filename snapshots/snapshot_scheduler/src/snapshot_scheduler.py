"""
Publish a new SnapshotJob to SNS.
"""

import datetime
import os

import attr
import boto3
from wellcome_aws_utils.lambda_utils import log_on_error
from wellcome_aws_utils.sns_utils import publish_sns_message


@attr.s
class S3ObjectLocation:
    bucket = attr.ib()
    key = attr.ib()


# This class is duplicated in the snapshot generator app
# Changes here will need to be reflected there, and vice versa.
@attr.s
class SnapshotJob(object):
    s3Location = attr.ib(type=S3ObjectLocation)
    apiVersion = attr.ib(type=str)
    requestedAt = attr.ib(type=datetime.datetime)


@log_on_error
def main(event=None, _ctxt=None, sns_client=None):
    print(os.environ)
    sns_client = sns_client or boto3.client("sns")

    topic_arn = os.environ["TOPIC_ARN"]

    public_bucket_name = os.environ["PUBLIC_BUCKET_NAME"]
    public_object_key_v2 = os.environ["PUBLIC_OBJECT_KEY_V2"]

    for (api_version, public_object_key) in [("v2", public_object_key_v2)]:
        snapshot_job = SnapshotJob(
            s3Location=S3ObjectLocation(
                bucket=public_bucket_name, key=public_object_key_v2
            ),
            apiVersion=api_version,
            requestedAt=datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        )

        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=attr.asdict(snapshot_job),
            subject="source: snapshot_scheduler.main",
        )
