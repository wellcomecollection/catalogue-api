package weco.api.search

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.search.config.MultiClusterConfigParser
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.elasticsearch.ResilientElasticClient
import weco.api.search.models.{ApiConfig, ApiEnvironment, ClusterConfig}
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.typesafe.config.builders.EnrichConfig.RichConfig

import scala.util.{Failure, Success, Try}

object Main extends WellcomeTypesafeApp {

  runWithConfig { config: Config =>
    implicit val apiConfig: ApiConfig = ApiConfig.build(config)
    implicit val clock: java.time.Clock = java.time.Clock.systemUTC()
    implicit val actorSystem: ActorSystem = ActorSystem("search-api")
    implicit val ec: scala.concurrent.ExecutionContext = actorSystem.dispatcher

    apiConfig.environment match {
      case ApiEnvironment.Dev =>
        info(s"Running in dev mode.")
      case _ =>
        info(s"Running in deployed mode (environment=${apiConfig.environment})")
        // Only initialise tracing in deployed environments
        Tracing.init(config)
    }

    val pipelineDate = apiConfig.environment match {
      case ApiEnvironment.Dev =>
        val pipelineDateOverride = config.getStringOption("dev.pipelineDate")
        if (pipelineDateOverride.isDefined)
          warn(s"Overridden pipeline date: $pipelineDateOverride")
        pipelineDateOverride.getOrElse(ClusterConfig.defaultPipelineDate)
      case _ =>
        ClusterConfig.defaultPipelineDate
    }

    def buildElasticClient(config: ClusterConfig): ResilientElasticClient =
      new ResilientElasticClient(
        clientFactory = () =>
          PipelineElasticClientBuilder(
            clusterConfig = config,
            serviceName = "catalogue_api",
            environment = apiConfig.environment,
            pipelineDate = config.getPipelineDate
        )
      )

    val clusterConfig = ClusterConfig(pipelineDate = Some(pipelineDate))
    val elasticClient = buildElasticClient(clusterConfig)

    // Create additional non-essential Elasticsearch clients (if configured) for routing experimental queries.
    // Catch all errors so that misconfigured experimental clusters do not cause the production API to crash.
    val parsedAdditionalClusterConfigs = MultiClusterConfigParser.parse(config)
    val additionalClusters =
      parsedAdditionalClusterConfigs.toList.flatMap {
        case (name, config) =>
          Try(buildElasticClient(config)) match {
            case Success(client) =>
              info(s"Configured additional Elasticsearch cluster '$name'")
              Some((name, (client, config)))
            case Failure(_) =>
              error(s"Failed to build additional Elasticsearch cluster '$name'")
              None
          }
      }.toMap

    info(
      s"Using default Elasticsearch cluster with ${additionalClusters.size} additional cluster(s)")

    val router = new SearchApi(
      elasticClient = elasticClient,
      clusterConfig = clusterConfig,
      additionalElasticClients = additionalClusters.map {
        case (name, (client, _)) => name -> client
      },
      additionalClusterConfigs = additionalClusters.map {
        case (name, (_, cfg)) => name -> cfg
      },
      apiConfig = apiConfig
    )

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
