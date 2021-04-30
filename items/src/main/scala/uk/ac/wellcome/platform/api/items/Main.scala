package uk.ac.wellcome.platform.api.items

import java.net.URL

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.monitoring.typesafe.CloudWatchBuilder
import uk.ac.wellcome.platform.api.common.http.WellcomeHttpApp
import uk.ac.wellcome.platform.api.common.http.config.builders.HTTPServerBuilder
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.common.services.config.builders.StacksServiceBuilder
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import weco.http.monitoring.HttpMetrics
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig.RichConfig

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val asMain: ActorSystem =
      AkkaBuilder.buildActorSystem()

    implicit val ecMain: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    Tracing.init(config)

    val workService: StacksService = StacksServiceBuilder.build(config)

    val apiConf =
      ApiConfig(
        host = config
          .getStringOption("api.host")
          .getOrElse("localhost"),
        scheme = config
          .getStringOption("api.scheme")
          .getOrElse("https"),
        defaultPageSize = config
          .getIntOption("api.pageSize")
          .getOrElse(10),
        pathPrefix =
          s"${config.getStringOption("api.apiName").getOrElse("catalogue")}",
        contextSuffix = config
          .getStringOption("api.context.suffix")
          .getOrElse("context.json")
      )

    val router = new ItemsApi {
      override implicit val ec: ExecutionContext = ecMain
      override implicit val stacksWorkService: StacksService = workService
      override implicit val apiConfig: ApiConfig = apiConf

      override def context: String = contextUri
    }

    val appName = "ItemsApi"

    new WellcomeHttpApp(
      routes = router.routes,
      httpMetrics = new HttpMetrics(
        name = appName,
        metrics = CloudWatchBuilder.buildCloudWatchMetrics(config)
      ),
      httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
      contextURL = new URL(router.contextUri),
      appName = appName
    )
  }
}
