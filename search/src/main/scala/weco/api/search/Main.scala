package weco.api.search

import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.models.{ApiConfig, ApiEnvironment, DevEnvironment, PipelineClusterElasticConfig}
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      ActorSystem("main-actor-system")
    implicit val executionContext: ExecutionContext =
      actorSystem.dispatcher

    implicit val apiConfig: ApiConfig = ApiConfig.build(config)

    // Don't initialise tracing in dev environments
    if (apiConfig.environment == ApiEnvironment.Dev) {
      info(s"Running in dev mode @ ${apiConfig.publicHost}")
    } else {
      info(s"Running in deployed mode @ ${apiConfig.publicHost}")
      Tracing.init(config)
    }

    val elasticClient = PipelineElasticClientBuilder(
      serviceName = "catalogue_api",
      environment = apiConfig.environment
    )

    val elasticConfig = PipelineClusterElasticConfig()

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
