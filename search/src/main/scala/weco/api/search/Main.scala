package weco.api.search

import org.apache.pekko.http.scaladsl.server.Directives.concat
import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.api.search.config.MultiClusterConfigParser
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.elasticsearch.ResilientElasticClient
import weco.api.search.models.ApiConfig
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val apiConfig: ApiConfig = ApiConfig.build(config)
    implicit val clock: java.time.Clock = java.time.Clock.systemUTC()
    implicit val actorSystem: ActorSystem = ActorSystem("search-api")
    implicit val ec: scala.concurrent.ExecutionContext = actorSystem.dispatcher

    val (elasticClient, elasticConfig) =
      ElasticClientSetup.buildDefaultElasticClientAndConfig(
        config = config,
        serviceName = "catalogue_api",
        environment = apiConfig.environment
      )

    // Parse multi-cluster configuration
    val additionalClusterConfigs =
      MultiClusterConfigParser.parseMultiClusterConfig(config)

    // Create clients for additional clusters
    val additionalClients = additionalClusterConfigs.map {
      case (clusterName, clusterConfig) =>
        info(s"Initializing client for cluster: $clusterName")
        val client = new ResilientElasticClient(
          clientFactory = () =>
            PipelineElasticClientBuilder(
              clusterConfig = clusterConfig,
              serviceName = "catalogue_api",
              environment = apiConfig.environment
          )
        )
        clusterName -> client
    }

    info(
      s"Using multi-cluster router with ${additionalClients.size} additional cluster(s)")

    val defaultRouter = new SearchApi(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      apiConfig = apiConfig
    )
    val additionalRouter = new MultiClusterSearchApi(
      additionalClients = additionalClients,
      apiConfig = apiConfig,
      additionalClusterConfigs = additionalClusterConfigs
    )
    val allRoutes = Seq(additionalRouter.routes, defaultRouter.routes)

    val appName = "SearchApi"

    new WellcomeHttpApp(
      routes = concat(allRoutes: _*),
      httpMetrics = new HttpMetrics(
        name = appName,
        metrics = CloudWatchBuilder.buildCloudWatchMetrics(config)
      ),
      httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
      appName = appName
    )
  }
}
