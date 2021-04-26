package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.alpakka.s3.S3Settings
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.Suite
import uk.ac.wellcome.platform.snapshot_generator.services.SnapshotService
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.snapshot_generator.models.SnapshotGeneratorConfig

import scala.concurrent.ExecutionContext.Implicits.global

trait SnapshotServiceFixture extends IndexFixtures { this: Suite =>
  def withSnapshotService[R](s3AkkaSettings: S3Settings,
                             worksIndex: Index = "worksIndex",
                             elasticClient: ElasticClient = elasticClient)(
    testWith: TestWith[SnapshotService, R])(
    implicit actorSystem: ActorSystem): R = {

    val snapshotService = new SnapshotService(
      akkaS3Settings = s3AkkaSettings,
      elasticClient = elasticClient,
      snapshotConfig = SnapshotGeneratorConfig(index = worksIndex)
    )

    testWith(snapshotService)
  }
}
