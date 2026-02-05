package weco.api.search

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.search.config.MultiClusterConfigParser
import weco.api.search.config.builders.{
  MultiClusterElasticClientBuilder,
  PipelineElasticClientBuilder
}
import weco.api.search.models.{
  ApiConfig,
  ApiEnvironment,
  ElasticConfig,
  PipelineClusterElasticConfig
}
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.typesafe.config.builders.EnrichConfig.RichConfig
import weco.api.search.elasticsearch.ResilientElasticClient

/**
  * Main entry point for the Search API with multi-cluster support.
  *
  * This extends the standard Main with the ability to route requests to multiple
  * Elasticsearch clusters based on configuration.
  *
  * To use this instead of the standard Main, update your deployment to use
  * `weco.api.search.MultiClusterMain` as the main class.
  */
object MultiClusterMain extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val apiConfig: ApiConfig = ApiConfig.build(config)
    implicit val clock: java.time.Clock = java.time.Clock.systemUTC()
    implicit val actorSystem: ActorSystem = ActorSystem("search-api")
    implicit val ec: scala.concurrent.ExecutionContext = actorSystem.dispatcher

    // Set up default cluster (existing behavior)
    val (defaultElasticClient, defaultElasticConfig) =
      apiConfig.environment match {
        case ApiEnvironment.Dev =>
          info(s"Running in dev mode.")
          val pipelineDateOverride = config.getStringOption("dev.pipelineDate")
          val pipelineDate =
            pipelineDateOverride.getOrElse(ElasticConfig.pipelineDate)
          if (pipelineDateOverride.isDefined)
            warn(s"Overridden pipeline date: $pipelineDate")
          (
            new ResilientElasticClient(
              clientFactory = () =>
                PipelineElasticClientBuilder(
                  serviceName = "catalogue_api",
                  pipelineDate = pipelineDate,
                  environment = apiConfig.environment
              )),
            PipelineClusterElasticConfig(
              config.getStringOption("dev.pipelineDate")
            )
          )
        case _ =>
          info(
            s"Running in deployed mode (environment=${apiConfig.environment})")
          Tracing.init(config)
          (
            new ResilientElasticClient(
              clientFactory = () =>
                PipelineElasticClientBuilder(
                  serviceName = "catalogue_api",
                  environment = apiConfig.environment
              )),
            PipelineClusterElasticConfig()
          )
      }

    // Parse multi-cluster configuration
    val multiClusterConfig = MultiClusterConfigParser.parseMultiClusterConfig(
      config,
      defaultElasticConfig
    )

    // Create clients for additional clusters
    val additionalClients = multiClusterConfig.additionalClusters.map {
      case (clusterName, clusterConfig) =>
        info(s"Initializing client for cluster: $clusterName")
        val client = new ResilientElasticClient(
          clientFactory = () =>
            MultiClusterElasticClientBuilder.buildClient(
              clusterConfig = clusterConfig,
              serviceName = "catalogue_api",
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
        defaultElasticClient = defaultElasticClient,
        defaultElasticConfig = defaultElasticConfig,
        additionalClients = additionalClients,
        multiClusterConfig = multiClusterConfig,
        apiConfig = apiConfig
      )
    } else {
      info(
        "No additional clusters configured, using standard single-cluster router")
      new SearchApi(
        elasticClient = defaultElasticClient,
        elasticConfig = defaultElasticConfig,
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
