package uk.ac.wellcome.platform.api.requests

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.Index
import com.typesafe.config.Config
import uk.ac.wellcome.api.display.ElasticConfig
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.http.typesafe.HTTPServerBuilder
import uk.ac.wellcome.monitoring.typesafe.CloudWatchBuilder
import uk.ac.wellcome.platform.api.common.services.{SierraService, StacksService}
import uk.ac.wellcome.platform.api.common.services.config.builders.{SierraServiceBuilder, StacksServiceBuilder}
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import weco.api.stacks.services.ItemLookup
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val asMain: ActorSystem =
      AkkaBuilder.buildActorSystem()

    val apiConf =
      ApiConfig(
        publicHost = config
          .getStringOption("api.host")
          .getOrElse("localhost"),
        publicScheme = config
          .getStringOption("api.scheme")
          .getOrElse("https"),
        defaultPageSize = 10,
        publicRootPath =
          s"${config.getStringOption("api.apiName").getOrElse("catalogue")}",
        contextPath = config
          .getStringOption("api.context.suffix")
          .getOrElse("context.json")
      )

    val elasticClient = ElasticBuilder.buildElasticClient(config)

    val router: RequestsApi = new RequestsApi {
      override implicit val ec: ExecutionContext = AkkaBuilder.buildExecutionContext()
      override implicit val stacksWorkService: StacksService =
        StacksServiceBuilder.build(config)
      override implicit val apiConfig: ApiConfig = apiConf
      override val sierraService: SierraService =
        SierraServiceBuilder.build(config)
      override val itemLookup: ItemLookup = ItemLookup(elasticClient)
      override val index: Index = ElasticConfig.apply().worksIndex
    }

    implicit val ec: ExecutionContext = router.ec

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
