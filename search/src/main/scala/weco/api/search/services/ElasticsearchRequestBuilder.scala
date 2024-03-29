package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.SearchRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.ElasticDsl._
import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest
import weco.api.search.models.SearchOptions

trait ElasticsearchRequestBuilder[S <: SearchOptions[_, _]] {
  val idSort: FieldSort

  // return Either because Images and countWorkTypes still use the old way.
  // Eventually, this should only return a TemplateSearchRequest.
  def request(
    searchOptions: S,
    index: Index
  ): Either[SearchRequest, TemplateSearchRequest]
}

object ElasticsearchRequestBuilder {

  def includesExcludesQuery(
    field: String,
    includes: List[String],
    excludes: List[String]
  ): Query =
    (includes, excludes) match {
      case (_, Nil) =>
        termsQuery(field = field, values = includes)
      case (Nil, _) =>
        not(termsQuery(field = field, values = excludes))
      case _ =>
        must(
          termsQuery(field = field, values = includes),
          not(termsQuery(field = field, values = excludes))
        )
    }
}
