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
            key=f"{public_object_key_prefix}/{doc_type}.json.gz"
        ),
        requestedAt=datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        bulkSize=es_bulk_size,
        index=Index(name=index),
        pipelineDate=pipeline_date,
        query=query
    )


def get_pipeline_date(*, current_index):
    ends_in_date_with_optional_suffix = re.compile(r'\d{4}-\d{2}-\d{2}.?$')
    match = ends_in_date_with_optional_suffix.search(current_index)
    return match[0]


def get_snapshot_jobs(indices):
    pipeline_date = get_pipeline_date(current_index=indices["works"])
    works_job = snapshot_job(
        index=indices["works"],
        doc_type="works",
        pipeline_date=pipeline_date,
        query=json.dumps({
            "term": {
                "type": "Visible"
            }
        })
    )
    images_job = snapshot_job(
        index=indices["images"],
        doc_type="images",
        pipeline_date=pipeline_date,
        query=None
    )
    return [works_job, images_job]
