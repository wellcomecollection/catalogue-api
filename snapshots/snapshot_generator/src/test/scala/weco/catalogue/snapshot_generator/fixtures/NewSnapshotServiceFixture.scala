package weco.catalogue.snapshot_generator.fixtures

import com.sksamuel.elastic4s.{ElasticClient, Index}
import org.scalatest.Suite
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import weco.catalogue.snapshot_generator.NewSnapshotService

trait NewSnapshotServiceFixture extends IndexFixtures with S3Fixtures { this: Suite =>
  def withSnapshotService[R](worksIndex: Index = "worksIndex",
                             elasticClient: ElasticClient = elasticClient)(
                              testWith: TestWith[NewSnapshotService, R]): R =
    testWith(
      new NewSnapshotService(
        index = worksIndex,
      )(elasticClient, s3Client)
    )
}
