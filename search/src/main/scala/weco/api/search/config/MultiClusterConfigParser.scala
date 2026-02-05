package weco.api.search.config

import com.typesafe.config.Config
import weco.api.search.models.{ClusterConfig, MultiClusterElasticConfig, ElasticConfig}
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
  def parseMultiClusterConfig(
    config: Config,
    defaultElasticConfig: ElasticConfig
  ): MultiClusterElasticConfig = {
    
    val defaultCluster = ClusterConfig(
      name = "default",
      worksIndex = Some(defaultElasticConfig.worksIndex),
      imagesIndex = Some(defaultElasticConfig.imagesIndex),
      pipelineDate = Some(defaultElasticConfig.pipelineDate.date)
    )
    
    // Check if multiCluster configuration exists
    if (!config.hasPath("multiCluster")) {
      info("No multi-cluster configuration found, using default cluster only")
      return MultiClusterElasticConfig(
        defaultCluster = defaultCluster,
        additionalClusters = Map.empty[String, ClusterConfig]
      )
    }
    
    val multiClusterConfig = config.getConfig("multiCluster")
    val clusterNames = multiClusterConfig.root().keySet().asScala.toSet
    
    info(s"Found multi-cluster configuration for clusters: ${clusterNames.mkString(", ")}")
    
    val additionalClusters: Map[String, ClusterConfig] = clusterNames.map { clusterName =>
      val clusterConfig = parseClusterConfig(clusterName, multiClusterConfig.getConfig(clusterName))
      clusterName -> clusterConfig
    }.toMap
    
    MultiClusterElasticConfig(
      defaultCluster = defaultCluster,
      additionalClusters = additionalClusters
    )
  }
  
  private def parseClusterConfig(name: String, config: Config): ClusterConfig = {
    val pipelineDate = getStringOption(config, "pipelineDate")
    val worksIndex = getStringOption(config, "worksIndex").map(Index(_))
    val imagesIndex = getStringOption(config, "imagesIndex").map(Index(_))
    
    val customHost = getStringOption(config, "customHost")
    val customPort = getIntOption(config, "customPort")
    val customProtocol = getStringOption(config, "customProtocol")
    val customApiKeySecretPath = getStringOption(config, "customApiKeySecretPath")
    
    info(s"Parsed cluster config '$name': " +
      s"pipelineDate=$pipelineDate, " +
      s"worksIndex=$worksIndex, " +
      s"customHost=$customHost")
    
    ClusterConfig(
      name = name,
      worksIndex = worksIndex,
      imagesIndex = imagesIndex,
      pipelineDate = pipelineDate,
      customHost = customHost,
      customPort = customPort,
      customProtocol = customProtocol,
      customApiKeySecretPath = customApiKeySecretPath
    )
  }
  
  private def getStringOption(config: Config, path: String): Option[String] = {
    if (config.hasPath(path)) Some(config.getString(path)) else None
  }
  
  private def getIntOption(config: Config, path: String): Option[Int] = {
    if (config.hasPath(path)) Some(config.getInt(path)) else None
  }
}
