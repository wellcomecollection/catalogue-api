package weco.catalogue.snapshot_generator.models

import java.time.Instant
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import weco.catalogue.display_model.models.ApiVersions

case class SnapshotJob(
  s3Location: S3ObjectLocation,
  apiVersion: ApiVersions.Value,
  requestedAt: Instant
)
