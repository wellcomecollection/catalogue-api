package weco.catalogue.snapshot_generator.fixtures

import com.sksamuel.elastic4s.{ElasticClient, Index}
import org.scalatest.Suite
import weco.fixtures.TestWith
import weco.catalogue.internal_model.index.IndexFixtures
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import weco.catalogue.snapshot_generator.models.SnapshotGeneratorConfig
import weco.catalogue.snapshot_generator.services.SnapshotService

trait SnapshotServiceFixture extends IndexFixtures with S3Fixtures {
  this: Suite =>
  def withSnapshotService[R](worksIndex: Index = "worksIndex",
                             elasticClient: ElasticClient = elasticClient)(
    testWith: TestWith[SnapshotService, R]): R =
    testWith(
      new SnapshotService(
        SnapshotGeneratorConfig(index = worksIndex)
      )(elasticClient, s3Client)
    )
}
