package weco.api.search.models

sealed trait SimilarityMetric

object SimilarityMetric {
  case object Blended extends SimilarityMetric
  case object Features extends SimilarityMetric
  case object Colors extends SimilarityMetric
}
