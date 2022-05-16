package weco.api.search.models.display

import io.circe.Encoder
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.WorkAggregations
import weco.api.search.models.request.WorkAggregationRequest
import weco.json.JsonUtil._

case class DisplayWorkAggregations(
  workType: Option[DisplayAggregation],
  `production.dates`: Option[DisplayAggregation],
  `genres.label`: Option[DisplayAggregation],
  `subjects.label`: Option[DisplayAggregation],
  `contributors.agent.label`: Option[DisplayAggregation],
  languages: Option[DisplayAggregation],
  `items.locations.license`: Option[DisplayAggregation],
  availabilities: Option[DisplayAggregation],
  @JsonKey("type") ontologyType: String = "Aggregations"
)

object DisplayWorkAggregations {
  implicit def encoder: Encoder[DisplayWorkAggregations] =
    deriveConfiguredEncoder

  def apply(
    aggs: WorkAggregations,
    aggregationRequests: Seq[WorkAggregationRequest]
  ): DisplayWorkAggregations =
    DisplayWorkAggregations(
      workType = aggs.format.map(DisplayAggregation(_)),
      `production.dates` =
        aggs.productionDates.map(DisplayAggregation(_)),
      `genres.label` =
        aggs.genresLabel.map(DisplayAggregation(_)),
      languages = aggs.languages.map(DisplayAggregation(_)),
      `subjects.label` = aggs.subjectsLabel.map(DisplayAggregation(_)),
      `contributors.agent.label` = aggs.contributorsAgentsLabel.map(DisplayAggregation(_)),
      `items.locations.license` = aggs.itemsLocationsLicense.map(DisplayAggregation(_)),
      availabilities = aggs.availabilities.map(DisplayAggregation(_))
    )
}
