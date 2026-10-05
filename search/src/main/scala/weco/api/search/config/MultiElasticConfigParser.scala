package weco.api.search.config

import com.typesafe.config.Config
import weco.api.search.models.ElasticConfig
import weco.typesafe.config.builders.EnrichConfig.RichConfig

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import grizzled.slf4j.Logging

object MultiElasticConfigParser extends Logging {

  /**
    * Parse multi-cluster Elasticsearch configuration from Typesafe Config.
    *
    * Looks for configuration keys like:
    *   multiCluster.xp-a.apiKeySecretPath="elasticsearch/xp-a/api_key"
    */
  def parse(config: Config): Map[String, ElasticConfig] = {
    // Check if multiCluster configuration exists
    if (!config.hasPath("multiCluster")) {
      info("No multi-cluster configuration found, using default cluster only")
      return Map.empty[String, ElasticConfig]
    }

    val multiElasticConfig = config.getConfig("multiCluster")
    val clusterNames = multiElasticConfig.root().keySet().asScala.toSet

    info(
      s"Found multi-cluster configuration for clusters: ${clusterNames.mkString(", ")}"
    )

    clusterNames.flatMap {
      clusterName =>
        val config = multiElasticConfig.getConfig(clusterName)

        // Additional clusters are not essential. If a cluster config fails to parse for any reason,
        // fail gracefully and exclude it from the returned map.
        Try(parseElasticConfig(clusterName, config)) match {
          case Success(elasticConfig) =>
            Some(clusterName -> elasticConfig)
          case Failure(e) =>
            error(
              s"Could not parse cluster config for '$clusterName': ${e.getMessage}"
            )
            None
        }
    }.toMap
  }

  private def parseElasticConfig(
    clusterName: String,
    config: Config
  ): ElasticConfig =
    ElasticConfig(
      name = clusterName,
      worksIndex = config.getStringOption("worksIndex"),
      imagesIndex = config.getStringOption("imagesIndex"),
      hostSecretPath = Some(config.getString("hostSecretPath")),
      apiKeySecretPath = Some(config.getString("apiKeySecretPath")),
      portSecretPath = config.getStringOption("portSecretPath"),
      protocolSecretPath = config.getStringOption("protocolSecretPath")
    )
}
