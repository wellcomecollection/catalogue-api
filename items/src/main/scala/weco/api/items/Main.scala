package weco.api.items

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.Uri
import com.typesafe.config.Config
import weco.Tracing
import weco.api.items.config.builders.SierraOauthHttpClientBuilder
import weco.api.items.services.{
  ItemUpdateService,
  SierraItemUpdater,
  VenueOpeningTimesLookup,
  WorkLookup
}
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.api.search.models.{ApiConfig, ApiEnvironment}
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.client.{HttpGet, PekkoHttpClient}
import weco.http.monitoring.HttpMetrics
import weco.sierra.http.SierraSource

import java.time.{Clock, ZoneId}
import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      ActorSystem("main-actor-system")
    implicit val executionContext: ExecutionContext =
      actorSystem.dispatcher

    implicit val apiConfig: ApiConfig = ApiConfig.build(config)

    apiConfig.environment match {
      case ApiEnvironment.Dev =>
        info(s"Running in dev mode.")
      case _ =>
        info(s"Running in deployed mode (environment=${apiConfig.environment})")
        // Only initialise tracing in deployed environments
        Tracing.init(config)
    }

    // We don't actually care about the hold limit in the items service.
    val client = SierraOauthHttpClientBuilder.build(
      config = config,
      environment = apiConfig.environment
    )
    val sierraSource = new SierraSource(client)

    val contentHttpClient = new PekkoHttpClient() with HttpGet {
      override val baseUri: Uri = config.getString("content.api.publicRoot")
    }
    val venueOpeningTimeLookup = new VenueOpeningTimesLookup(contentHttpClient)
    val venueClock = Clock.system(ZoneId.of("Europe/London"))

    // To add an item updater for a new service:
    // implement ItemUpdater and add it to the list here
    val itemUpdaters = List(
      new SierraItemUpdater(
        sierraSource,
        venueOpeningTimeLookup,
        venueClock
      )
    )

    val itemUpdateService = new ItemUpdateService(itemUpdaters)

    val catalogueHttpClient = new PekkoHttpClient() with HttpGet {
      override val baseUri: Uri = config.getString("catalogue.api.publicRoot")
    }

    val router = new ItemsApi(
      itemUpdateService = itemUpdateService,
      workLookup = new WorkLookup(catalogueHttpClient)
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
