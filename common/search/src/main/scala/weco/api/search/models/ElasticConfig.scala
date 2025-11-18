package weco.api.search.models

import com.sksamuel.elastic4s.Index
import grizzled.slf4j.Logging

case class ElasticConfig(
  worksIndex: Index,
  imagesIndex: Index
)

object ElasticConfig {
  // We use this to share config across Scala API applications
  // i.e. The API and the snapshot generator.
  val indexDateWorks = "2025-10-09"
  val indexDateImages = "2025-10-02"
}

object PipelineClusterElasticConfig extends Logging {
  def apply(overrideDate: Option[String] = None): ElasticConfig = {
    val indexDateWorks = overrideDate.getOrElse(ElasticConfig.indexDateWorks)
    val indexDateImages = overrideDate.getOrElse(ElasticConfig.indexDateImages)

    info(
      s"Using works index date $indexDateWorks and images index date $indexDateImages.")

    ElasticConfig(
      worksIndex = Index(s"works-indexed-$indexDateWorks"),
      imagesIndex = Index(s"images-indexed-$indexDateImages")
    )
  }
}
