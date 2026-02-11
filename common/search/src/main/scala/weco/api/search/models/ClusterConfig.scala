package weco.api.search.models

/** Configuration for a single Elasticsearch cluster */
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
)

sealed trait VectorType
object VectorType {
  case object Dense extends VectorType
  case object Sparse extends VectorType
}

case class SemanticConfig(
  modelId: String,
  vectorType: VectorType,
  k: Int,
  numCandidates: Int
)
