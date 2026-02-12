package weco.api.search

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.search.config.MultiClusterConfigParser
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.elasticsearch.ResilientElasticClient
import weco.api.search.models.{ApiConfig, ApiEnvironment, ElasticConfig}
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.typesafe.config.builders.EnrichConfig.RichConfig

import scala.util.Try

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
        pipelineDateOverride.getOrElse(ElasticConfig.defaultPipelineDate)
      case _ =>
        ElasticConfig.defaultPipelineDate
    }

    def buildElasticClient(config: ElasticConfig): ResilientElasticClient =
      new ResilientElasticClient(
        clientFactory = () =>
          PipelineElasticClientBuilder(
            clusterConfig = config,
            serviceName = "catalogue_api",
            environment = apiConfig.environment,
            pipelineDate = config.getPipelineDate
        )
      )

    val clusterConfig = ElasticConfig(pipelineDate = Some(pipelineDate))
    val elasticClient = buildElasticClient(clusterConfig)

    val parsedAdditionalClusterConfigs = MultiClusterConfigParser.parse(config)
    val (additionalElasticClients, additionalClusterConfigs) =
      parsedAdditionalClusterConfigs.foldLeft(
        (
          Map.empty[String, ResilientElasticClient],
          Map.empty[String, ElasticConfig])
      ) {
        case ((clients, configs), (name, currConfig)) =>
          Try(buildElasticClient(currConfig))
            .map { client =>
              info(s"Configured additional Elasticsearch cluster '$name'")
              (clients + (name -> client), configs + (name -> currConfig))
            }
            .getOrElse {
              error(s"Failed to build additional Elasticsearch cluster '$name'")
              (clients, configs)
            }
      }

    info(
      s"Using default Elasticsearch cluster with ${additionalClusterConfigs.size} additional cluster(s)")

    val router = new SearchApi(
      elasticClient = elasticClient,
      clusterConfig = clusterConfig,
      additionalElasticClients = additionalElasticClients,
      additionalClusterConfigs = additionalClusterConfigs,
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
