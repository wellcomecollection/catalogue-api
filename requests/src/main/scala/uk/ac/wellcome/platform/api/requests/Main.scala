package uk.ac.wellcome.platform.api.requests

import java.net.URL
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.Index
import com.typesafe.config.Config
import uk.ac.wellcome.api.display.ElasticConfig
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.http.typesafe.HTTPServerBuilder
import uk.ac.wellcome.monitoring.typesafe.CloudWatchBuilder
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.common.services.config.builders.StacksServiceBuilder
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.stacks.services.ItemLookup
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

    val itLookup = new ItemLookup(
      elasticsearchService = new ElasticsearchService(elasticClient)
    )

    val router: RequestsApi = new RequestsApi {
      override implicit val ec: ExecutionContext = ecMain
      override implicit val stacksWorkService: StacksService = workService
      override implicit val apiConfig: ApiConfig = apiConf

      override def context: String = contextUri

      override implicit val itemLookup: ItemLookup = itLookup
      override val index: Index = ElasticConfig.apply().worksIndex
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
