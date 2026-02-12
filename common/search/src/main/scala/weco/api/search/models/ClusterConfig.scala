package weco.api.search.models

import com.sksamuel.elastic4s.Index

case class ClusterConfig(
  name: String = "default",
  pipelineDate: Option[String] = None,
  worksIndex: Option[String] = None,
  imagesIndex: Option[String] = None,
  hostSecretPath: Option[String] = None,
  apiKeySecretPath: Option[String] = None,
  portSecretPath: Option[String] = None,
  protocolSecretPath: Option[String] = None,
  semanticConfig: Option[SemanticConfig] = None
) {
  
  def getPipelineDate: String =
    pipelineDate.getOrElse(ClusterConfig.defaultPipelineDate)

  def getWorksIndex: Index =
    Index(worksIndex.getOrElse(
      s"works-indexed-${ClusterConfig.defaultWorksIndexDate}"))

  def getImagesIndex: Index =
    Index(imagesIndex.getOrElse(
      s"images-indexed-${ClusterConfig.defaultImagesIndexDate}"))
}

object ClusterConfig {
  // Default values shared across the API
  // We use this to share config across Scala API applications
  // i.e. The API and the snapshot generator.
  val defaultPipelineDate = "2025-10-02"
  val defaultWorksIndexDate = "2025-11-20"
  val defaultImagesIndexDate = "2025-10-02"
}

sealed trait VectorType
object VectorType {
  case object Dense extends VectorType
  case object Sparse extends VectorType
}

case class SemanticConfig(
  modelId: String,
  vectorType: VectorType,
  k: Int = 50,
  numCandidates: Int = 500,
  rankWindowSize: Int = 10000,
  rankConstant: Int = 20,
)
