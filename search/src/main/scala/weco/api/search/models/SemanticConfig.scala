package weco.api.search.models

sealed trait VectorType
object VectorType {
  case object Dense extends VectorType
  case object Sparse extends VectorType
}

case class SemanticConfig(
  modelId: String,
  vectorType: VectorType
)
