package uk.ac.wellcome.platform.api.items

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.Index
import com.typesafe.config.Config
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.api.display.ElasticConfig
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.http.typesafe.HTTPServerBuilder
import uk.ac.wellcome.monitoring.typesafe.CloudWatchBuilder
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.common.services.config.builders.SierraServiceBuilder
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import weco.http.monitoring.HttpMetrics
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig.RichConfig
import weco.api.stacks.services.WorkLookup
import weco.http.WellcomeHttpApp

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val asMain: ActorSystem =
      AkkaBuilder.buildActorSystem()

    Tracing.init(config)

    val apiConf =
      ApiConfig(
        publicHost = config
          .getStringOption("api.host")
          .getOrElse("localhost"),
        publicScheme = config
          .getStringOption("api.scheme")
          .getOrElse("https"),
        defaultPageSize = config
          .getIntOption("api.pageSize")
          .getOrElse(10),
        publicRootPath =
          s"${config.getStringOption("api.apiName").getOrElse("catalogue")}",
        contextPath = config
          .getStringOption("api.context.suffix")
          .getOrElse("context.json")
      )

    val elasticClient = ElasticBuilder.buildElasticClient(config)

    val router = new ItemsApi {
      override implicit val ec: ExecutionContext =
        AkkaBuilder.buildExecutionContext()
      override implicit val apiConfig: ApiConfig = apiConf

      override val index: Index = ElasticConfig.apply().worksIndex
      override val sierraService: SierraService =
        SierraServiceBuilder.build(config)
      override val workLookup: WorkLookup = WorkLookup(elasticClient)
    }

    val appName = "ItemsApi"

    implicit val ec: ExecutionContext = router.ec

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
