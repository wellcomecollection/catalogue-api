package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.Query
import weco.api.search.models.request.SortingOrder

case class SearchTemplateParams(
  query: Option[String],
  from: Int,
  size: Int,
  sortByDate: Option[SortingOrder],
  sortField: Option[String],
  sortByScore: Boolean,
  includes: Seq[String],
  aggs: Seq[AbstractAggregation],
  preFilter: Seq[Query],
  postFilter: Option[Query],
  knn: Option[SearchTemplateKNNParams] = None,
  includeSemantic: Boolean = false,
  semanticModelId: Option[String] = None
)

case class SearchTemplateKNNParams(
  field: String,
  k: Int,
  numCandidates: Int,
  queryVector: Seq[Double],
  similarityThreshold: Option[Double] = None
)
