package weco.api.search.models

sealed trait SimilarityMetric

object SimilarityMetric {
  object Features extends SimilarityMetric
}
