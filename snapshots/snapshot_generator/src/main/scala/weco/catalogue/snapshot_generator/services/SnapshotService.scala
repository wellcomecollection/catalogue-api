package weco.catalogue.snapshot_generator.services

import com.amazonaws.services.s3.AmazonS3
import com.sksamuel.elastic4s.{ElasticClient, Index}
import grizzled.slf4j.Logging
import uk.ac.wellcome.display.models.DisplayWork
import weco.catalogue.snapshot_generator.compress.GzipCompressor
import weco.catalogue.snapshot_generator.iterators.{
  ElasticsearchWorksIterator,
  WorkToJsonIterator
}
import weco.catalogue.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob,
  SnapshotResult
}
import weco.catalogue.snapshot_generator.storage.S3Uploader

import java.time.Instant
import scala.util.Try

class SnapshotService(index: Index)(
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
      visibleWorks <- Try {
        new ElasticsearchWorksIterator()(elasticClient).scroll(index)
          .map { work =>
            workCount += 1
            work
          }
      }

      jsonStrings = WorkToJsonIterator(visibleWorks)

      compressedBytes = GzipCompressor(jsonStrings)
        .map { byte =>
          s3Size += 1
          byte
        }

      uploadResult <- uploader.upload(snapshotJob.s3Location, compressedBytes)

      snapshotResult = SnapshotResult(
        indexName = index.name,
        documentCount = workCount,
        startedAt = startedAt,
        finishedAt = Instant.now(),
        displayModel = DisplayWork.getClass.getCanonicalName,
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
