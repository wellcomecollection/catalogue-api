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
  val pipelineDate = "2024-06-06"
}

object PipelineClusterElasticConfig extends Logging {
  def apply(overrideDate: Option[String] = None): ElasticConfig = {
    val date = overrideDate.getOrElse(ElasticConfig.pipelineDate)

    info(s"Using pipeline date: $date")

    ElasticConfig(
      worksIndex = Index(s"works-indexed-$date"),
      imagesIndex = Index(s"images-indexed-$date")
    )
  }
}
