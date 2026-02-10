package weco.api.search

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.search.config.MultiClusterConfigParser
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.elasticsearch.ResilientElasticClient
import weco.api.search.models.{ApiConfig, ApiEnvironment, ClusterConfig, ElasticConfig}
import weco.typesafe.WellcomeTypesafeApp
import weco.http.WellcomeHttpApp
import weco.http.monitoring.HttpMetrics
import weco.http.typesafe.HTTPServerBuilder
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.typesafe.config.builders.EnrichConfig.RichConfig

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
        pipelineDateOverride.getOrElse(ElasticConfig.pipelineDate)
      case _ =>
        ElasticConfig.pipelineDate
    }

    val clusterConfig = ClusterConfig(pipelineDate = Some(pipelineDate))
    val elasticClient = new ResilientElasticClient(
      clientFactory = () =>
        PipelineElasticClientBuilder(
          clusterConfig = clusterConfig,
          serviceName = "catalogue_api",
          environment = apiConfig.environment
        )
    )

    val additionalClusterConfigs =
      MultiClusterConfigParser.parseMultiClusterConfig(config)
    val additionalElasticClients = additionalClusterConfigs.map {
      case (name, clusterConfig) =>
        val client = new ResilientElasticClient(
          clientFactory = () =>
            PipelineElasticClientBuilder(
              clusterConfig = clusterConfig,
              serviceName = "catalogue_api",
              environment = apiConfig.environment
            )
        )
        name -> client
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
