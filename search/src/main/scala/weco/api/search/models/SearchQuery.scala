package weco.api.search.models

case class SearchQuery(query: String, queryType: SearchQueryType)
object SearchQuery {
  def apply(query: String,
            maybeQueryType: Option[SearchQueryType]): SearchQuery =
    SearchQuery(query, maybeQueryType.getOrElse(SearchQueryType.default))

  def apply(query: String): SearchQuery =
    SearchQuery(query, None)
}
