package weco.api.snapshot_generator.fixtures

import com.sksamuel.elastic4s.{ElasticClient, Index}
import org.scalatest.Suite
import weco.api.search.fixtures.IndexFixtures
import weco.api.snapshot_generator.models.SnapshotJob
import weco.api.snapshot_generator.services.SnapshotService
import weco.fixtures.TestWith
import weco.storage.fixtures.S3Fixtures
import weco.storage.s3.S3ObjectLocation

import java.time.Instant

trait SnapshotServiceFixture extends IndexFixtures with S3Fixtures {
  this: Suite =>
  def createSnapshotJob(
    s3Location: S3ObjectLocation,
    index: Index,
    pipelineDate: String = "test",
    bulkSize: Int = 1000,
    requestedAt: Instant = Instant.now(),
    query: Option[String] = None
  ): SnapshotJob = SnapshotJob(
    s3Location = s3Location,
    requestedAt = requestedAt,
    index = index,
    pipelineDate = pipelineDate,
    bulkSize = bulkSize,
    query = query
  )

  def withSnapshotService[R](
    elasticClient: ElasticClient = elasticClient
  )(testWith: TestWith[SnapshotService, R]): R =
    testWith(
      new SnapshotService(_ => elasticClient, s3Client)
    )
}

object SnapshotServiceFixture {
  val visibleTermQuery: Option[String] = Some(
    """{ "term": { "type": "Visible" } }"""
  )
}
