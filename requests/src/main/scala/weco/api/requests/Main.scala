package weco.api.requests

import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.elasticsearch.typesafe.ElasticBuilder
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.api.stacks.services.builders.SierraServiceBuilder
import weco.api.search.models.ApiConfig
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.api.stacks.services.elastic.ElasticItemLookup
import weco.catalogue.display_model.ElasticConfig
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val asMain: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val ec: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    implicit val apiConfig: ApiConfig = ApiConfig.build(config)

    val elasticClient = ElasticBuilder.buildElasticClient(config)

    val router: RequestsApi = new RequestsApi(
      sierraService = SierraServiceBuilder.build(config),
      itemLookup = ElasticItemLookup(
        elasticClient,
        index = ElasticConfig.apply().worksIndex
      )
    )

    val appName = "RequestsApi"

    new WellcomeHttpApp(
      routes = router.routes,
      httpMetrics = new HttpMetrics(
        name = appName,
        metrics = CloudWatchBuilder.buildCloudWatchMetrics(config)
      ),
      httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
      contextUrl = router.contextUrl,
      appName = appName
    )
  }
}
