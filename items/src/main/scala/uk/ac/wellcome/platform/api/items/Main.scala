package uk.ac.wellcome.platform.api.items

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.api.display.ElasticConfig
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.http.typesafe.HTTPServerBuilder
import uk.ac.wellcome.monitoring.typesafe.CloudWatchBuilder
import uk.ac.wellcome.platform.api.common.services.config.builders.SierraServiceBuilder
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import weco.api.stacks.services.WorkLookup
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val asMain: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val ec: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    Tracing.init(config)

    implicit val apiConfig: ApiConfig = ApiConfig.build(config)

    val elasticClient = ElasticBuilder.buildElasticClient(config)

    val router = new ItemsApi(
      sierraService = SierraServiceBuilder.build(config),
      workLookup = WorkLookup(elasticClient),
      index = ElasticConfig.apply().worksIndex
    )

    val appName = "ItemsApi"

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
