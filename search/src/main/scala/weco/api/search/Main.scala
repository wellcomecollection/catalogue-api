package weco.api.search

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.api.search.config.MultiClusterConfigParser
import weco.api.search.config.builders.CustomElasticClientBuilder
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
        serviceName = "catalogue_api"
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
            CustomElasticClientBuilder(
              clusterConfig = clusterConfig,
              environment = apiConfig.environment
            )
        )
        clusterName -> client
    }

    // Choose router based on whether we have additional clusters configured
    val router: ApiRouter = if (additionalClients.nonEmpty) {
      info(
        s"Using multi-cluster router with ${additionalClients.size} additional cluster(s)")
      new MultiClusterSearchApi(
        defaultElasticClient = elasticClient,
        defaultElasticConfig = elasticConfig,
        additionalClients = additionalClients,
        apiConfig = apiConfig,
        additionalClusterConfigs = additionalClusterConfigs
      )
    } else {
      info(
        "No additional clusters configured, using standard single-cluster router")
      new SearchApi(
        elasticClient = elasticClient,
        elasticConfig = elasticConfig,
        apiConfig = apiConfig
      )
    }

    val appName = "SearchApi"

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
