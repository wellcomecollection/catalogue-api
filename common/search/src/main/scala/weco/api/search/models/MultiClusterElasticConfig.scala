package weco.api.search.models

import com.sksamuel.elastic4s.Index

/** Configuration for a single Elasticsearch cluster */
case class ClusterConfig(
  name: String,
  worksIndex: Option[Index] = None,
  imagesIndex: Option[Index] = None,
  pipelineDate: Option[String] = None,
  // For serverless or custom clusters that don't follow the pipeline pattern
  customHost: Option[String] = None,
  customPort: Option[Int] = None,
  customProtocol: Option[String] = None,
  customApiKeySecretPath: Option[String] = None,
  semanticModelId: Option[String] = None,
  semanticVectorType: Option[String] = None
)

/** Multi-cluster configuration supporting both pipeline and custom clusters */
case class MultiClusterElasticConfig(
  // Default cluster (existing behavior)
  defaultCluster: ClusterConfig,
  // Additional clusters mapped by name
  additionalClusters: Map[String, ClusterConfig] = Map.empty
) {
  
  /** Get cluster config by name, falling back to default */
  def getCluster(name: String): ClusterConfig = 
    additionalClusters.getOrElse(name, defaultCluster)
  
  /** Get all cluster names including default */
  def allClusterNames: Set[String] = 
    additionalClusters.keySet + defaultCluster.name
}

object MultiClusterElasticConfig {
  
  /** Create a simple single-cluster config from existing ElasticConfig */
  def fromElasticConfig(elasticConfig: ElasticConfig): MultiClusterElasticConfig = {
    MultiClusterElasticConfig(
      defaultCluster = ClusterConfig(
        name = "default",
        worksIndex = Some(elasticConfig.worksIndex),
        imagesIndex = Some(elasticConfig.imagesIndex),
        pipelineDate = Some(elasticConfig.pipelineDate.date)
      )
    )
  }
}
