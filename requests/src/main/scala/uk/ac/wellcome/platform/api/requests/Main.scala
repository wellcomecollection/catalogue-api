package uk.ac.wellcome.platform.api.requests

import java.net.URL

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.typesafe.CloudWatchBuilder
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.common.services.config.builders.StacksServiceBuilder
import uk.ac.wellcome.platform.api.http.config.builders.HTTPServerBuilder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val asMain: ActorSystem =
      AkkaBuilder.buildActorSystem()

    implicit val ecMain: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val workService: StacksService = StacksServiceBuilder.build(config)

    val router: RequestsApi = new RequestsApi {
      override implicit val ec: ExecutionContext = ecMain
      override implicit val stacksWorkService: StacksService = workService
    }

    val appName = "RequestsApi"

    new WellcomeHttpApp(
      routes = router.routes,
      httpMetrics = new HttpMetrics(
        name = appName,
        metrics = CloudWatchBuilder.buildCloudWatchMetrics(config)
      ),
      httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
      contextURL =
        new URL("https://api.wellcomecollection.org/stacks/v1/context.json"),
      appName = appName
    )
  }
}
