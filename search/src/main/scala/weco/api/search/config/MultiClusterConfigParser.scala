package weco.api.search.config

import com.typesafe.config.Config
import weco.api.search.models.ClusterConfig
import weco.typesafe.config.builders.EnrichConfig.RichConfig

import com.sksamuel.elastic4s.Index
import scala.collection.JavaConverters._
import grizzled.slf4j.Logging

object MultiClusterConfigParser extends Logging {

  /**
    * Parse multi-cluster configuration from Typesafe Config.
    *
    * Looks for configuration keys like:
    *   multiCluster.xp-a.pipelineDate="2025-11-20"
    *   multiCluster.xp-a.worksIndex="works-indexed-2025-11-20"
    *   multiCluster.xp-a.customHost="serverless.es.aws.elastic.cloud"
    *   multiCluster.xp-a.customApiKeySecretPath="elasticsearch/xp-a/api_key"
    */
  def parseMultiClusterConfig(config: Config): Map[String, ClusterConfig] = {
    // Check if multiCluster configuration exists
    if (!config.hasPath("multiCluster")) {
      info("No multi-cluster configuration found, using default cluster only")
      return Map.empty[String, ClusterConfig]
    }

    val multiClusterConfig = config.getConfig("multiCluster")
    val clusterNames = multiClusterConfig.root().keySet().asScala.toSet

    info(
      s"Found multi-cluster configuration for clusters: ${clusterNames.mkString(", ")}")

    clusterNames.map { clusterName =>
      val config = multiClusterConfig.getConfig(clusterName)
      val clusterConfig = ClusterConfig(
        name = clusterName,
        worksIndex = Some(Index(config.getString("worksIndex"))),
        hostSecretPath = Some(config.getString("hostSecretPath")),
        apiKeySecretPath = Some(config.getString("apiKeySecretPath")),
        semanticModelId = config.getStringOption("semanticModelId"),
        semanticVectorType = config.getStringOption("semanticVectorType"),
      )
      clusterName -> clusterConfig
    }.toMap
  }
}
