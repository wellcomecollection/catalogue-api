package weco.api.search

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import grizzled.slf4j.Logging
import weco.Tracing
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.elasticsearch.ResilientElasticClient
import weco.api.search.models.{
  ApiEnvironment,
  ElasticConfig,
  PipelineClusterElasticConfig
}
import weco.typesafe.config.builders.EnrichConfig.RichConfig

object ElasticClientSetup extends Logging {

  def buildDefaultElasticClientAndConfig(
    config: Config,
    serviceName: String,
    environment: ApiEnvironment
  )(implicit
    actorSystem: ActorSystem,
    clock: java.time.Clock,
    ec: scala.concurrent.ExecutionContext)
    : (ResilientElasticClient, ElasticConfig) =
    environment match {
      case ApiEnvironment.Dev =>
        info(s"Running in dev mode.")
        val pipelineDateOverride = config.getStringOption("dev.pipelineDate")
        val pipelineDate =
          pipelineDateOverride.getOrElse(ElasticConfig.pipelineDate)
        if (pipelineDateOverride.isDefined)
          warn(s"Overridden pipeline date: $pipelineDate")
        (
          new ResilientElasticClient(
            clientFactory = () =>
              PipelineElasticClientBuilder(
                serviceName = serviceName,
                pipelineDate = pipelineDate,
                environment = environment
            )),
          PipelineClusterElasticConfig(
            config.getStringOption("dev.pipelineDate")
          )
        )
      case _ =>
        info(s"Running in deployed mode (environment=$environment)")
        // Only initialise tracing in deployed environments
        Tracing.init(config)
        (
          new ResilientElasticClient(
            clientFactory = () =>
              PipelineElasticClientBuilder(
                serviceName = serviceName,
                environment = environment
            )),
          PipelineClusterElasticConfig()
        )
    }
}
