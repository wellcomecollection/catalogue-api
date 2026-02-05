package weco.api.search

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.models.{
  ApiConfig,
  ApiEnvironment,
  ElasticConfig,
  PipelineClusterElasticConfig
}
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.typesafe.config.builders.EnrichConfig.RichConfig

import weco.api.search.elasticsearch.ResilientElasticClient

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val apiConfig: ApiConfig = ApiConfig.build(config)
    implicit val clock: java.time.Clock = java.time.Clock.systemUTC()
    implicit val actorSystem: ActorSystem = ActorSystem("search-api")
    implicit val ec: scala.concurrent.ExecutionContext = actorSystem.dispatcher

    val semanticWorksIndex = Option("works-openai-full")

    val (elasticClient, elasticConfig) = apiConfig.environment match {
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
                serviceName = "catalogue_api",
                pipelineDate = pipelineDate,
                environment = apiConfig.environment,
                useExperimentalSemanticSearchCluster = semanticWorksIndex.isDefined
              )),
          PipelineClusterElasticConfig(
            config.getStringOption("dev.pipelineDate"),
            semanticSearchIndexOverride = semanticWorksIndex
          )
        )
      case _ =>
        info(s"Running in deployed mode (environment=${apiConfig.environment})")
        // Only initialise tracing in deployed environments
        Tracing.init(config)
        (
          new ResilientElasticClient(
            clientFactory = () =>
              PipelineElasticClientBuilder(
                serviceName = "catalogue_api",
                environment = apiConfig.environment,
                useExperimentalSemanticSearchCluster = semanticWorksIndex.isDefined
              )),
          PipelineClusterElasticConfig(
            semanticSearchIndexOverride = semanticWorksIndex
          )
        )
    }

    val router = new SearchApi(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      apiConfig = apiConfig
    )

    val appName = "SearchApi"

    new WellcomeHttpApp(
      routes = router.routes,
      httpMetrics = new HttpMetrics(
        name = appName,
        metrics = CloudWatchBuilder.buildCloudWatchMetrics(config)
      ),
      httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
      appName = appName
    )
  }
}
