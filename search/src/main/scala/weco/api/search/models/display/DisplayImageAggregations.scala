package weco.api.search.models.display

import io.circe.Encoder
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.ImageAggregations
import weco.json.JsonUtil._

case class DisplayImageAggregations(
  license: Option[DisplayAggregation],
  `source.contributors.agent.label`: Option[DisplayAggregation],
  `source.genres.label`: Option[DisplayAggregation],
  `source.subjects.label`: Option[DisplayAggregation],
  @JsonKey("type") ontologyType: String = "Aggregations"
)

object DisplayImageAggregations {
  implicit def encoder: Encoder[DisplayImageAggregations] =
    deriveConfiguredEncoder

  def apply(aggs: ImageAggregations): DisplayImageAggregations =
    DisplayImageAggregations(
      license = aggs.license.map(DisplayAggregation(_)),
      `source.contributors.agent.label` =
        aggs.sourceContributorAgents.map(DisplayAggregation(_)),
      `source.genres.label` = aggs.sourceGenres.map(DisplayAggregation(_)),
      `source.subjects.label` = aggs.sourceSubjects.map(DisplayAggregation(_))
    )
}
