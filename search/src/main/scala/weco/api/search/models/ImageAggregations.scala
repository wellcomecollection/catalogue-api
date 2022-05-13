package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Json

case class ImageAggregations(
  license: Option[Aggregation[Json]] = None,
  sourceContributorAgents: Option[Aggregation[Json]] = None,
  sourceGenres: Option[Aggregation[Json]] = None
)

object ImageAggregations extends ElasticAggregations {
  def apply(searchResponse: SearchResponse): Option[ImageAggregations] = {
    val e4sAggregations = searchResponse.aggregations
    if (e4sAggregations.data.nonEmpty) {
      Some(
        ImageAggregations(
          license = e4sAggregations.decodeJsonAgg("license"),
          sourceContributorAgents = e4sAggregations.decodeJsonAgg("sourceContributorAgents"),
          sourceGenres = e4sAggregations.decodeJsonAgg("sourceGenres")
        )
      )
    } else {
      None
    }
  }
}
