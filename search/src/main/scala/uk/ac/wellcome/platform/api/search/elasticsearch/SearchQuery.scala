package uk.ac.wellcome.platform.api.search.elasticsearch

import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery

trait SearchQuery {
  def apply(query: String): BoolQuery
}