package uk.ac.wellcome.platform.api.search.models

case class ResultList[Result, Aggs](
  results: List[Result],
  totalResults: Int,
  aggregations: Option[Aggs]
)
