package weco.api.snapshot_generator.models

import java.time.Instant

import weco.storage.providers.s3.S3ObjectLocation

case class SnapshotResult(
  indexName: String,
  documentCount: Int,
  startedAt: Instant,
  finishedAt: Instant,
  s3Etag: String,
  s3Size: Long,
  s3Location: S3ObjectLocation
)
