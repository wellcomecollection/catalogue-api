package weco.api.items

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import weco.Tracing
import weco.api.items.services.{
  ItemUpdateService,
  SierraItemUpdater,
  WorkLookup
}
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.api.search.models.ApiConfig
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.client.{AkkaHttpClient, HttpGet}
import weco.http.monitoring.HttpMetrics
import weco.sierra.http.SierraSource
import weco.sierra.typesafe.SierraOauthHttpClientBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      ActorSystem("main-actor-system")
    implicit val executionContext: ExecutionContext =
      actorSystem.dispatcher

    Tracing.init(config)

    implicit val apiConfig: ApiConfig = ApiConfig.build(config)

    // We don't actually care about the hold limit in the items service.
    val client = SierraOauthHttpClientBuilder.build(config)
    val sierraSource = new SierraSource(client)

    // To add an item updater for a new service:
    // implement ItemUpdater and add it to the list here
    val itemUpdaters = List(
      new SierraItemUpdater(sierraSource)
    )

    val itemUpdateService = new ItemUpdateService(itemUpdaters)

    val httpClient = new AkkaHttpClient() with HttpGet {
      override val baseUri: Uri = config.getString("catalogue.api.publicRoot")
    }

    val router = new ItemsApi(
      itemUpdateService = itemUpdateService,
      workLookup = new WorkLookup(httpClient)
    )

    val appName = "ItemsApi"

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
