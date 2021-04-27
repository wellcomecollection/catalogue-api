package weco.catalogue.snapshot_generator

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import uk.ac.wellcome.display.ElasticConfig
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.storage.typesafe.S3Builder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import weco.catalogue.snapshot_generator.models.SnapshotGeneratorConfig
import weco.catalogue.snapshot_generator.services.{
  SnapshotGeneratorWorkerService,
  SnapshotService
}

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val snapshotConfig = SnapshotGeneratorConfig(
      index = ElasticConfig().worksIndex,
      bulkSize = config.getIntOption("es.bulk-size").getOrElse(1000)
    )

    implicit val elasticClient: ElasticClient = ElasticBuilder.buildElasticClient(config)

    implicit val s3Client: AmazonS3 = S3Builder.buildS3Client(config)

    val snapshotService = new SnapshotService(snapshotConfig)

    new SnapshotGeneratorWorkerService(
      snapshotService = snapshotService,
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      messageSender = SNSBuilder.buildSNSMessageSender(
        config,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage",
      )
    )
  }
}
