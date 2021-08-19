package weco.api.items

import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.items.services.{ItemUpdateService, SierraItemUpdater, WorkLookup}
import weco.elasticsearch.typesafe.ElasticBuilder
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.api.search.models.{ApiConfig, CheckModel}
import weco.api.stacks.http.SierraSource
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.catalogue.display_model.ElasticConfig
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.sierra.typesafe.SierraOauthHttpClientBuilder

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

    // We don't actually care about the hold limit in the items service.
    val client = SierraOauthHttpClientBuilder.build(config)
    val sierraSource = new SierraSource(client)

    // To add an item updater for a new service:
    // implement ItemUpdater and add it to the list here
    val itemUpdaters = List(
      new SierraItemUpdater(sierraSource)
    )

    val itemUpdateService = new ItemUpdateService(itemUpdaters)

    val router = new ItemsApi(
      itemUpdateService = itemUpdateService,
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
      appName = appName
    )
  }
}
