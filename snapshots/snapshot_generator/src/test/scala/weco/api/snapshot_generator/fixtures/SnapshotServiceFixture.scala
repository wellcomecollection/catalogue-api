package weco.api.snapshot_generator.fixtures

import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.Suite
import weco.api.search.fixtures.IndexFixtures
import weco.api.snapshot_generator.services.SnapshotService
import weco.fixtures.TestWith
import weco.storage.fixtures.S3Fixtures

trait SnapshotServiceFixture extends IndexFixtures with S3Fixtures {
  this: Suite =>
  def withSnapshotService[R](
    elasticClient: ElasticClient = elasticClient
  )(testWith: TestWith[SnapshotService, R]): R =
    testWith(
      new SnapshotService(elasticClient, s3Client)
    )
}

object SnapshotServiceFixture {
  val visibleTermQuery: Option[String] = Some(
    """{ "term": { "type": "Visible" } }"""
  )
}
