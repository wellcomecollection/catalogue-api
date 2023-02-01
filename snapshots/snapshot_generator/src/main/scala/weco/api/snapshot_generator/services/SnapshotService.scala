package weco.api.snapshot_generator.services

import com.sksamuel.elastic4s.ElasticClient
import grizzled.slf4j.Logging
import software.amazon.awssdk.services.s3.S3Client
import weco.api.snapshot_generator.compress.GzipCompressor
import weco.api.snapshot_generator.iterators.ElasticsearchIterator
import weco.api.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob,
  SnapshotResult
}
import weco.api.snapshot_generator.storage.S3Uploader

import java.time.Instant
import scala.util.Try

class SnapshotService(
  elasticClient: ElasticClient,
  implicit val s3Client: S3Client
) extends Logging {
  private val uploader = new S3Uploader()

  def generateSnapshot(job: SnapshotJob): Try[CompletedSnapshotJob] = {
    info(s"Running $job")

    val startedAt = Instant.now

    var workCount = 0
    var s3Size = 0L

    for {
      jsonStrings <- Try {
        new ElasticsearchIterator()(elasticClient)
          .scroll(
            index = job.index,
            bulkSize = job.bulkSize,
            query = job.query
          )
          .map { work =>
            workCount += 1
            work
          }
      }

      compressedBytes = GzipCompressor(jsonStrings)
        .map { byte =>
          s3Size += 1
          byte
        }

      uploadResult <- uploader.upload(job.s3Location, compressedBytes)

      snapshotResult = SnapshotResult(
        indexName = job.index.name,
        documentCount = workCount,
        startedAt = startedAt,
        finishedAt = Instant.now(),
        s3Etag = uploadResult.eTag(),
        s3Size = s3Size,
        s3Location = job.s3Location
      )

      completedJob = CompletedSnapshotJob(
        snapshotJob = job,
        snapshotResult = snapshotResult
      )
    } yield completedJob
  }
}
