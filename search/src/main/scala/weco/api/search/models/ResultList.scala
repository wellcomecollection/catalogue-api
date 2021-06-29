package weco.api.search.models

case class ResultList[Result, Aggs](
  results: List[Result],
  totalResults: Int,
  aggregations: Option[Aggs]
)
