package weco.api.requests

import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.elasticsearch.typesafe.ElasticBuilder
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.api.search.models.{ApiConfig, CheckModel}
import weco.api.stacks.services.SierraService
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.api.stacks.services.elastic.ElasticItemLookup
import weco.catalogue.display_model.ElasticConfig
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.sierra.typesafe.SierraOauthHttpClientBuilder
import weco.typesafe.config.builders.EnrichConfig._

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
    val elasticConfig = ElasticConfig()

    CheckModel.checkModel(elasticConfig.worksIndex.name)(elasticClient)

    val holdLimit = config.requireInt("sierra.holdLimit")
    val client = SierraOauthHttpClientBuilder.build(config)
    val sierraService = SierraService(client, holdLimit = holdLimit)

    val router: RequestsApi = new RequestsApi(
      sierraService = sierraService,
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
      appName = appName
    )
  }
}
