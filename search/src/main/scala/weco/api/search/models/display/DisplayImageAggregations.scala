package weco.api.search.models.display

import io.circe.{Encoder, Json}
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.{Aggregation, ImageAggregations}
import weco.json.JsonUtil._

case class DisplayImageAggregations(
  license: Option[DisplayAggregation[Json]],
  `source.contributors.agent.label`: Option[DisplayAggregation[Json]],
  `source.genres.label`: Option[DisplayAggregation[Json]],
  @JsonKey("type") ontologyType: String = "Aggregations"
)

object DisplayImageAggregations {
  implicit def encoder: Encoder[DisplayImageAggregations] =
    deriveConfiguredEncoder

  def apply(aggs: ImageAggregations): DisplayImageAggregations =
    DisplayImageAggregations(
      license = displayAggregation(aggs.license, identity[Json]),
      `source.contributors.agent.label` =
        displayAggregation(aggs.sourceContributorAgents, identity[Json]),
      `source.genres.label` =
        displayAggregation(aggs.sourceGenres, identity[Json])
    )

  private def displayAggregation[T, D](
    maybeAgg: Option[Aggregation[T]],
    display: T => D
  ): Option[DisplayAggregation[D]] =
    maybeAgg.map {
      DisplayAggregation(_, display)
    }
}
