package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Json

case class WorkAggregations(
  format: Option[Aggregation[Json]] = None,
  genresLabel: Option[Aggregation[Json]] = None,
  productionDates: Option[Aggregation[Json]] = None,
  languages: Option[Aggregation[Json]] = None,
  subjectsLabel: Option[Aggregation[Json]] = None,
  contributorsAgentsLabel: Option[Aggregation[Json]] = None,
  itemsLocationsLicense: Option[Aggregation[Json]] = None,
  availabilities: Option[Aggregation[Json]] = None
)

object WorkAggregations extends ElasticAggregations {
  def apply(searchResponse: SearchResponse): Option[WorkAggregations] = {
    val e4sAggregations = searchResponse.aggregations
    if (e4sAggregations.data.nonEmpty) {
      Some(
        WorkAggregations(
          format = e4sAggregations.decodeJsonAgg("format"),
          genresLabel = e4sAggregations.decodeJsonAgg("genres"),
          productionDates = e4sAggregations.decodeJsonAgg("productionDates"),
          languages = e4sAggregations.decodeJsonAgg("languages"),
          subjectsLabel = e4sAggregations.decodeJsonAgg("subjects"),
          // TODO decode only agents here once `contributors` is removed
          contributorsAgentsLabel = e4sAggregations.decodeJsonAgg("contributors"),
          itemsLocationsLicense = e4sAggregations.decodeJsonAgg("license"),
          availabilities = e4sAggregations.decodeJsonAgg("availabilities")
        )
      )
    } else {
      None
    }
  }
}
