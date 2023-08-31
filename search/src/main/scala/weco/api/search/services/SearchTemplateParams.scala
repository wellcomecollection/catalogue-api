package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.Query
import weco.api.search.models.request.SortingOrder

case class SearchTemplateParams(
  query: Option[String],
  from: Int,
  size: Int,
  sortByDate: Option[SortingOrder],
  sortByScore: Boolean,
  includes: Seq[String],
  aggs: Seq[AbstractAggregation],
  postFilter: Option[Query]
)