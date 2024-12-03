package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.SearchResponse

case class ImageAggregations(
  license: Option[Aggregation] = None,
  sourceContributorAgents: Option[Aggregation] = None,
  sourceGenres: Option[Aggregation] = None,
  sourceSubjects: Option[Aggregation] = None
)

object ImageAggregations extends ElasticAggregations {
  def apply(searchResponse: SearchResponse): Option[ImageAggregations] = {
    val e4sAggregations = searchResponse.aggregations
    if (e4sAggregations.data.nonEmpty) {
      Some(
        ImageAggregations(
          license = e4sAggregations.decodeAgg("license"),
          sourceContributorAgents =
            e4sAggregations.decodeAgg("sourceContributorAgents"),
          sourceGenres = e4sAggregations.decodeAgg("sourceGenres"),
          sourceSubjects = e4sAggregations.decodeAgg("sourceSubjects")
        )
      )
    } else {
      None
    }
  }
}
