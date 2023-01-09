package weco.api.snapshot_generator

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import software.amazon.awssdk.services.s3.S3Client
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.models.PipelineClusterElasticConfig
import weco.api.snapshot_generator.models.SnapshotGeneratorConfig
import weco.api.snapshot_generator.services.{
  SnapshotGeneratorWorkerService,
  SnapshotService
}
import weco.messaging.sns.NotificationMessage
import weco.messaging.typesafe.{SNSBuilder, SQSBuilder}
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val elasticConfig = PipelineClusterElasticConfig()

    val snapshotConfig = SnapshotGeneratorConfig(
      index = elasticConfig.worksIndex,
      bulkSize = config.getIntOption("es.bulk-size").getOrElse(1000)
    )

    implicit val elasticClient: ElasticClient =
      PipelineElasticClientBuilder("snapshot_generator")

    implicit val s3Client: S3Client = S3Client.builder().build()

    val snapshotService = new SnapshotService(snapshotConfig)

    new SnapshotGeneratorWorkerService(
      snapshotService = snapshotService,
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      messageSender = SNSBuilder.buildSNSMessageSender(
        config,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    )
  }
}
