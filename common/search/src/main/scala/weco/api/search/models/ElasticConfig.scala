package weco.api.search.models

import com.sksamuel.elastic4s.Index

case class ElasticConfig(
  worksIndex: Index,
  imagesIndex: Index
)

trait ElasticConfigBase {
  // We use this to share config across Scala API applications
  // i.e. The API and the snapshot generator.
  val pipelineDate = "2022-08-24" // Note: remember to update the TypeScript config also
}

object PipelineClusterElasticConfig extends ElasticConfigBase {
  def apply(): ElasticConfig =
    ElasticConfig(
      worksIndex = Index(s"works-indexed-$pipelineDate"),
      imagesIndex = Index(s"images-indexed-$pipelineDate")
    )
}
