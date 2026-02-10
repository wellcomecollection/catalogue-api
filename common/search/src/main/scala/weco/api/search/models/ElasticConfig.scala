package weco.api.search.models

import com.sksamuel.elastic4s.Index
import grizzled.slf4j.Logging

case class EsCluster(date: String)

case class ElasticConfig(
  worksIndex: Index,
  imagesIndex: Index,
  pipelineDate: EsCluster
)

object ElasticConfig {
  // We use this to share config across Scala API applications
  // i.e. The API and the snapshot generator.
  val pipelineDate = "2025-10-02"
  val indexDateWorks = "2025-11-20"
  val indexDateImages = "2025-10-02"
}

object PipelineClusterElasticConfig extends Logging {
  def apply(clusterConfig: ClusterConfig = ClusterConfig()): ElasticConfig = {
    val pipelineDate =
      clusterConfig.pipelineDate.getOrElse(ElasticConfig.pipelineDate)
    val worksIndex =
      clusterConfig.worksIndex.getOrElse(
        s"works-indexed-${ElasticConfig.indexDateWorks}")
    val imagesIndex =
      clusterConfig.imagesIndex.getOrElse(
        s"images-indexed-${ElasticConfig.indexDateImages}")

    info(
      s"Cluster name: ${clusterConfig.name}; pipelineDate: $pipelineDate; worksIndex: $worksIndex; imagesIndex: $imagesIndex"
    )

    ElasticConfig(
      worksIndex = Index(worksIndex),
      imagesIndex = Index(imagesIndex),
      pipelineDate = EsCluster(pipelineDate)
    )
  }
}
