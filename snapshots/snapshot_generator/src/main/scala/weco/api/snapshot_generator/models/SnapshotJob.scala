package weco.api.snapshot_generator.models

import weco.api.search.models.ApiVersions

import java.time.Instant
import weco.storage.s3.S3ObjectLocation

case class SnapshotJob(
  s3Location: S3ObjectLocation,
  apiVersion: ApiVersions.Value,
  requestedAt: Instant
)
