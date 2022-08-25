package weco.api.search

import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing

import weco.api.search.config.builders.{
  MetricsBuilder,
  PipelineElasticClientBuilder
}
import weco.api.search.models.{
  ApiConfig,
  PipelineClusterElasticConfig,
  QueryConfig
}
import weco.api.search.models.{ApiConfig, CheckModel, QueryConfig}
import weco.api.search.models.PipelineClusterElasticConfig

import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    Tracing.init(config)

    implicit val apiConfig: ApiConfig = ApiConfig.build(config)

    val elasticClient = PipelineElasticClientBuilder("catalogue_api")
    val elasticConfig = PipelineClusterElasticConfig()

    val queryConfig =
      QueryConfig.fetchFromIndex(elasticClient, elasticConfig.imagesIndex)

    val router = new SearchApi(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      queryConfig = queryConfig,
      apiConfig = apiConfig
    )

    val appName = "SearchApi"
    new WellcomeHttpApp(
      routes = router.routes,
      httpMetrics = new HttpMetrics(
        name = appName,
        metrics = MetricsBuilder(config)
      ),
      httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
      appName = appName
    )
  }
}
