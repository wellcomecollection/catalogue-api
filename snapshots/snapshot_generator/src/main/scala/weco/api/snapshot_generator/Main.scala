package weco.api.snapshot_generator

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import weco.api.snapshot_generator.config.builders.SnapshotGeneratorElasticClientBuilder
import weco.api.snapshot_generator.models.SnapshotGeneratorConfig
import weco.api.snapshot_generator.services.{
  SnapshotGeneratorWorkerService,
  SnapshotService
}
import weco.catalogue.display_model.ElasticConfig
import weco.messaging.sns.NotificationMessage
import weco.messaging.typesafe.{SNSBuilder, SQSBuilder}
import weco.storage.typesafe.S3Builder
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.typesafe.config.builders.EnrichConfig._

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

    implicit val elasticClient: ElasticClient =
      SnapshotGeneratorElasticClientBuilder()

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
