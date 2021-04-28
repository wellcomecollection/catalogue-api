package weco.catalogue.snapshot_generator.models

import java.time.Instant

import uk.ac.wellcome.api.display.models.ApiVersions
import uk.ac.wellcome.storage.s3.S3ObjectLocation

case class SnapshotJob(
  s3Location: S3ObjectLocation,
  apiVersion: ApiVersions.Value,
  requestedAt: Instant
)
