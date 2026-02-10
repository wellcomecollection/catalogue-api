package weco.api.search

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import weco.Tracing
import weco.api.search.config.MultiClusterConfigParser
import weco.api.search.config.builders.PipelineElasticClientBuilder
import weco.api.search.elasticsearch.ResilientElasticClient
import weco.api.search.models.{
  ApiConfig,
  ApiEnvironment,
  ClusterConfig,
  ElasticConfig,
  PipelineClusterElasticConfig
}
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

    val elasticConfig = apiConfig.environment match {
      case ApiEnvironment.Dev =>
        info(s"Running in dev mode.")
        val pipelineDateOverride = config.getStringOption("dev.pipelineDate")
        val pipelineDate =
          pipelineDateOverride.getOrElse(ElasticConfig.pipelineDate)
        if (pipelineDateOverride.isDefined)
          warn(s"Overridden pipeline date: $pipelineDate")

        PipelineClusterElasticConfig(
          ClusterConfig(
            pipelineDate = config.getStringOption("dev.pipelineDate"))
        )
      case _ =>
        info(s"Running in deployed mode (environment=${apiConfig.environment})")
        // Only initialise tracing in deployed environments
        Tracing.init(config)
        PipelineClusterElasticConfig()
    }

    val elasticClient = new ResilientElasticClient(
      clientFactory = () =>
        PipelineElasticClientBuilder(
          serviceName = "catalogue_api",
          pipelineDate = elasticConfig.pipelineDate.date,
          environment = apiConfig.environment
      ))

    // Parse multi-cluster configuration
    val additionalClusterConfigs =
      MultiClusterConfigParser.parseMultiClusterConfig(config)

    info(
      s"Using multi-cluster router with ${additionalClusterConfigs.size} additional cluster(s)")

    val router = new SearchApi(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      apiConfig = apiConfig,
      additionalClusterConfigs = additionalClusterConfigs
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
