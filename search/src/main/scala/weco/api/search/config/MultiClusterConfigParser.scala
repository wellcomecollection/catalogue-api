package weco.api.search.config

import com.typesafe.config.Config
import weco.api.search.models.{ClusterConfig, SemanticConfig, VectorType}
import weco.typesafe.config.builders.EnrichConfig.RichConfig

import scala.collection.JavaConverters._
import grizzled.slf4j.Logging

object MultiClusterConfigParser extends Logging {

  /**
    * Parse multi-cluster Elasticsearch configuration from Typesafe Config.
    *
    * Looks for configuration keys like:
    *   multiCluster.xp-a.apiKeySecretPath="elasticsearch/xp-a/api_key"
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
        worksIndex = config.getStringOption("worksIndex"),
        imagesIndex = config.getStringOption("imagesIndex"),
        hostSecretPath = Some(config.getString("hostSecretPath")),
        apiKeySecretPath = Some(config.getString("apiKeySecretPath")),
        portSecretPath = config.getStringOption("portSecretPath"),
        protocolSecretPath = config.getStringOption("protocolSecretPath"),
        semanticConfig = parseSemanticConfig(config)
      )
      clusterName -> clusterConfig
    }.toMap
  }

  private def parseSemanticConfig(config: Config): Option[SemanticConfig] = {
    if (!config.hasPath("semantic")) return None
    val semanticConfig = config.getConfig("semantic")

    val vectorType = semanticConfig.getString("vectorType") match {
      case "dense"  => Some(VectorType.Dense)
      case "sparse" => Some(VectorType.Sparse)
      case _        => None
    }

    vectorType.map { vt =>
      SemanticConfig(
        modelId = semanticConfig.getString("modelId"),
        vectorType = vt,
        k = semanticConfig.getIntOption("k").getOrElse(50),
        numCandidates =
          semanticConfig.getIntOption("numCandidates").getOrElse(500)
      )
    }
  }
}
