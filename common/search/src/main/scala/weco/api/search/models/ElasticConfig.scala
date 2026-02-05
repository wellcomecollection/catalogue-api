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
  def apply(overridePipelineDate: Option[String] = None,
            overrideWorksIndexDate: Option[String] = None,
            overrideImagesIndexDate: Option[String] = None,
            semanticSearchIndexOverride: Option[String] = None): ElasticConfig = {
    val pipelineDate =
      overridePipelineDate.getOrElse(ElasticConfig.pipelineDate)
    val indexDateWorks =
      overrideWorksIndexDate.getOrElse(ElasticConfig.indexDateWorks)
    val indexDateImages =
      overrideImagesIndexDate.getOrElse(ElasticConfig.indexDateImages)

    info(
      s"Using pipeline date $pipelineDate, works index date $indexDateWorks, and images index date $indexDateImages.")

    ElasticConfig(
      worksIndex = Index(semanticSearchIndexOverride.getOrElse(s"works-indexed-$indexDateWorks")),
      imagesIndex = Index(s"images-indexed-$indexDateImages"),
      pipelineDate = EsCluster(pipelineDate)
    )
  }
}
