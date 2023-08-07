package weco.api.snapshot_generator.models

import com.sksamuel.elastic4s.Index

import java.time.Instant
import weco.storage.providers.s3.S3ObjectLocation

case class SnapshotJob(
  s3Location: S3ObjectLocation,
  requestedAt: Instant,
  pipelineDate: String,
  index: Index,
  // How many documents should be fetched in a single request?
  //
  //  - If this value is too small, we have to make extra requests and
  //    snapshot creation will be slower.
  //
  //  - If this value is too big, we may exceed the heap memory on a single
  //    request -- >100MB in one set of returned works, and we get an error:
  //
  //        org.apache.http.ContentTooLongException: entity content is too
  //        long [167209080] for the configured buffer limit [104857600]
  //
  bulkSize: Int,
  // A query to filter documents when snapshotting an index - if None, all documents are used
  query: Option[String] = None
)
