package weco.api.requests

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import weco.Tracing
import weco.api.requests.services.{ItemLookup, RequestsService, SierraRequestsService}
import weco.api.search.models.ApiConfig
import weco.http.WellcomeHttpApp
import weco.http.client.{AkkaHttpClient, HttpGet}
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.sierra.typesafe.SierraOauthHttpClientBuilder
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
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

    val httpClient = new AkkaHttpClient() with HttpGet {
      override val baseUri: Uri = config.getString("catalogue.api.publicRoot")
    }

    val holdLimit = config.requireInt("sierra.holdLimit")
    val client = SierraOauthHttpClientBuilder.build(config)
    val sierraService = SierraRequestsService(client, holdLimit = holdLimit)
    val itemLookup = new ItemLookup(httpClient)

    val requestsService = new RequestsService(sierraService, itemLookup)

    val router: RequestsApi = new RequestsApi(requestsService)

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
