package weco.api.snapshot_generator

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import software.amazon.awssdk.services.s3.S3Client
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.models.ElasticConfig
import weco.api.snapshot_generator.models.PipelineElasticClient
import weco.api.snapshot_generator.services.{SnapshotGeneratorWorkerService, SnapshotService}
import weco.messaging.sns.NotificationMessage
import weco.messaging.typesafe.{SNSBuilder, SQSBuilder}
import weco.typesafe.WellcomeTypesafeApp

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      ActorSystem("main-actor-system")
    implicit val executionContext: ExecutionContext =
      actorSystem.dispatcher

    val s3Client: S3Client = S3Client.builder().build()

    val pipelineElasticClient: PipelineElasticClient =
      (pipelineDate: String) => PipelineElasticClientBuilder(
        ElasticConfig.forDefaultCluster(
          serviceName = "snapshot_generator",
          pipelineDate = pipelineDate
        )
      )

    val snapshotService =
      new SnapshotService(
        pipelineElasticClient,
        s3Client
      )

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
