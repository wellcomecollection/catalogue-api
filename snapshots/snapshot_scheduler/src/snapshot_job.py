import datetime
import json
import os
import re

import attr


@attr.s
class S3ObjectLocation:
    bucket = attr.ib()
    key = attr.ib()


@attr.s
class Index:
    name = attr.ib()


# This class is duplicated in the snapshot generator app
# Changes here will need to be reflected there, and vice versa.
@attr.s
class SnapshotJob(object):
    s3Location = attr.ib(type=S3ObjectLocation)
    index = attr.ib(type=Index)
    requestedAt = attr.ib()
    pipelineDate = attr.ib()
    bulkSize = attr.ib()
    query = attr.ib()


def snapshot_job(*, doc_type, index, pipeline_date, query):
    public_bucket_name = os.getenv("PUBLIC_BUCKET_NAME")
    public_object_key_prefix = os.getenv("PUBLIC_OBJECT_KEY_PREFIX")
    es_bulk_size = int(os.getenv("ES_BULK_SIZE"))

    return SnapshotJob(
        s3Location=S3ObjectLocation(
            bucket=public_bucket_name,
            key=f"{public_object_key_prefix}/{doc_type}.json.gz",
        ),
        requestedAt=datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        bulkSize=es_bulk_size,
        index=Index(name=index),
        pipelineDate=pipeline_date,
        query=query,
    )


def get_snapshot_jobs(elastic_config):
    works_job = snapshot_job(
        index=elastic_config["works"],
        doc_type="works",
        pipeline_date=elastic_config["pipelineDate"],
        query=json.dumps({"term": {"type": "Visible"}}),
    )
    images_job = snapshot_job(
        index=elastic_config["images"],
        doc_type="images",
        pipeline_date=elastic_config["pipelineDate"],
        query=None,
    )
    return [works_job, images_job]
