package weco.api.snapshot_generator

import software.amazon.awssdk.services.s3.S3Client
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.models.ApiEnvironment
import weco.api.snapshot_generator.models.SnapshotJob
import weco.api.snapshot_generator.services.SnapshotService
import weco.storage.providers.s3.S3ObjectLocation
import com.typesafe.config.{Config, ConfigFactory}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object MainLocal {
  def main(): Unit = {
    val config: Config = ConfigFactory.load()

    val snapshotService =
      new SnapshotService(
        PipelineElasticClientBuilder(
          "snapshot_generator",
          _,
          ApiEnvironment.Dev),
        S3Client.builder().build()
      )

    val snapshotJob = SnapshotJob(
      s3Location = S3ObjectLocation(
        bucket = config.getString("snapshotBucketName"),
        key = config.getString("snapshotBucketKey")
      ),
      requestedAt = Instant.now(),
      pipelineDate = config.getString("pipelineDate"),
      index = config.getString("snapshotIndex"),
      bulkSize = 150,
      query = Option(config.getString("snapshotQuery"))
    )
    snapshotService.generateSnapshot(snapshotJob);
  }
}
