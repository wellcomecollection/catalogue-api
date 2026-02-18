package weco.api.search.config

import com.sksamuel.elastic4s.Index
import com.typesafe.config.{Config, ConfigException}
import weco.api.search.models.{ElasticConfig, SemanticConfig, VectorType}
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
      s"Found multi-cluster configuration for clusters: ${clusterNames.mkString(", ")}")

    clusterNames.flatMap { clusterName =>
      val config = multiElasticConfig.getConfig(clusterName)

      // Additional clusters are not essential. If a cluster config fails to parse for any reason,
      // fail gracefully and exclude it from the returned map.
      Try(parseElasticConfig(clusterName, config)) match {
        case Success(elasticConfig) =>
          Some(clusterName -> elasticConfig)
        case Failure(e) =>
          error(
            s"Could not parse cluster config for '$clusterName': ${e.getMessage}")
          None
      }
    }.toMap
  }

  private def parseElasticConfig(clusterName: String,
                                 config: Config): ElasticConfig = {
    val semanticConfig =
      if (config.hasPath("semantic"))
        Some(parseSemanticConfig(config.getConfig("semantic")))
      else None

    ElasticConfig(
      name = clusterName,
      worksIndex = config.getStringOption("worksIndex").map(Index(_)),
      imagesIndex = config.getStringOption("imagesIndex").map(Index(_)),
      hostSecretPath = config.getString("hostSecretPath"),
      apiKeySecretPath = config.getString("apiKeySecretPath"),
      portSecretPath = config.getString("portSecretPath"),
      protocolSecretPath = config.getString("protocolSecretPath"),
      semanticConfig = semanticConfig
    )
  }

  private def parseSemanticConfig(config: Config): SemanticConfig = {
    val vectorType = config.getString("vectorType") match {
      case "dense"  => VectorType.Dense
      case "sparse" => VectorType.Sparse
      case other =>
        throw new ConfigException.BadValue(
          "semantic.vectorType",
          s"Invalid vectorType '$other'. Expected 'dense' or 'sparse'."
        )
    }
    val default = SemanticConfig(
      modelId = config.getString("modelId"),
      vectorType = vectorType)
    def intOrDefault(key: String, current: Int) =
      config.getIntOption(key).getOrElse(current)

    // Semantic search parameters are optional. Use defaults if not provided.
    default.copy(
      k = intOrDefault("k", default.k),
      numCandidates = intOrDefault("numCandidates", default.numCandidates),
      rankWindowSize = intOrDefault("rankWindowSize", default.rankWindowSize),
      rankConstant = intOrDefault("rankConstant", default.rankConstant)
    )
  }
}
