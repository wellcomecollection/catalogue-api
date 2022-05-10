package weco.api.snapshot_generator.services

import com.amazonaws.services.s3.AmazonS3
import com.sksamuel.elastic4s.ElasticClient
import grizzled.slf4j.Logging
import weco.api.snapshot_generator.compress.GzipCompressor
import weco.api.snapshot_generator.iterators.ElasticsearchWorksIterator
import weco.api.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotGeneratorConfig,
  SnapshotJob,
  SnapshotResult
}
import weco.api.snapshot_generator.storage.S3Uploader

import java.time.Instant
import scala.util.Try

class SnapshotService(config: SnapshotGeneratorConfig)(
  implicit
  elasticClient: ElasticClient,
  s3Client: AmazonS3
) extends Logging {
  private val uploader = new S3Uploader()

  def generateSnapshot(snapshotJob: SnapshotJob): Try[CompletedSnapshotJob] = {
    info(s"Running $snapshotJob")

    val startedAt = Instant.now

    var workCount = 0
    var s3Size = 0L

    for {
      jsonStrings <- Try {
        new ElasticsearchWorksIterator()(elasticClient)
          .scroll(config)
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

      uploadResult <- uploader.upload(snapshotJob.s3Location, compressedBytes)

      snapshotResult = SnapshotResult(
        indexName = config.index.name,
        documentCount = workCount,
        startedAt = startedAt,
        finishedAt = Instant.now(),
        s3Etag = uploadResult.getETag,
        s3Size = s3Size,
        s3Location = snapshotJob.s3Location
      )

      completedJob = CompletedSnapshotJob(
        snapshotJob = snapshotJob,
        snapshotResult = snapshotResult
      )
    } yield completedJob
  }
}
