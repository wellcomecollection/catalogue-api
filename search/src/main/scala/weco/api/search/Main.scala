package weco.api.search

import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.elasticsearch.typesafe.ElasticBuilder
import weco.Tracing
import weco.api.search.models.{ApiConfig, CheckModel, QueryConfig}
import weco.api.search.swagger.SwaggerDocs
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.catalogue.display_model.ApiClusterElasticConfig
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    Tracing.init(config)

    implicit val apiConfig: ApiConfig = ApiConfig.build(config)

    val elasticClient = ElasticBuilder.buildElasticClient(config)
    val elasticConfig = ApiClusterElasticConfig()

    CheckModel.checkModel(elasticConfig.worksIndex.name)(elasticClient)
    CheckModel.checkModel(elasticConfig.imagesIndex.name)(elasticClient)

    val queryConfig =
      QueryConfig.fetchFromIndex(elasticClient, elasticConfig.imagesIndex)

    val swaggerDocs = new SwaggerDocs(apiConfig)

    val router = new SearchApi(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      queryConfig = queryConfig,
      swaggerDocs = swaggerDocs,
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
