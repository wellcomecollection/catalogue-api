package weco.catalogue.snapshot_generator.models

case class CompletedSnapshotJob(
  snapshotJob: SnapshotJob,
  snapshotResult: SnapshotResult
)
