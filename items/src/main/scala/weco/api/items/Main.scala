package weco.api.items

import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.items.services.{ItemUpdateService, SierraItemUpdater, WorkLookup}
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.api.search.models.{ApiConfig, CheckModel}
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.catalogue.display_model.PipelineClusterElasticConfig
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.sierra.http.SierraSource
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

    val elasticClient = PipelineElasticClientBuilder("stacks_api")
    val elasticConfig = PipelineClusterElasticConfig()

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
      index = elasticConfig.worksIndex
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
