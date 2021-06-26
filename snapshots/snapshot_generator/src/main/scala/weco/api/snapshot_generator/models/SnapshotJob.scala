package weco.api.snapshot_generator.models

import java.time.Instant
import weco.storage.s3.S3ObjectLocation
import weco.catalogue.display_model.models.ApiVersions

case class SnapshotJob(
  s3Location: S3ObjectLocation,
  apiVersion: ApiVersions.Value,
  requestedAt: Instant
)
