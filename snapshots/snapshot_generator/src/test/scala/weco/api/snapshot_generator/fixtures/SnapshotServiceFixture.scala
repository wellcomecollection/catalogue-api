package weco.api.snapshot_generator.fixtures

import com.sksamuel.elastic4s.{ElasticClient, Index}
import org.scalatest.Suite
import weco.api.snapshot_generator.models.SnapshotGeneratorConfig
import weco.api.snapshot_generator.services.SnapshotService
import weco.fixtures.TestWith
import weco.catalogue.internal_model.index.IndexFixtures
import weco.storage.fixtures.S3Fixtures

trait SnapshotServiceFixture extends IndexFixtures with S3Fixtures {
  this: Suite =>
  def withSnapshotService[R](
    worksIndex: Index = "worksIndex",
    elasticClient: ElasticClient = elasticClient
  )(testWith: TestWith[SnapshotService, R]): R =
    testWith(
      new SnapshotService(
        SnapshotGeneratorConfig(index = worksIndex)
      )(elasticClient, s3Client)
    )
}
