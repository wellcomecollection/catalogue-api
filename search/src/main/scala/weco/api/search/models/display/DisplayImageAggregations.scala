package weco.api.search.models.display

import io.circe.Encoder
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models
import weco.api.search.models.{Aggregation, ImageAggregations}
import weco.catalogue.display_model.locations.DisplayLicense
import weco.catalogue.display_model.work.{DisplayAbstractAgent, DisplayGenre}
import weco.catalogue.internal_model.identifiers.IdState.Minted
import weco.catalogue.internal_model.work.{AbstractAgent, Genre}

case class DisplayImageAggregations(
  license: Option[DisplayAggregation[DisplayLicense]],
  `source.contributors.agent.label`: Option[
    DisplayAggregation[DisplayAbstractAgent]
  ] = None,
  `source.genres.label`: Option[DisplayAggregation[DisplayGenre]],
  @JsonKey("type") ontologyType: String = "Aggregations"
)

object DisplayImageAggregations {

  implicit def encoder: Encoder[DisplayImageAggregations] =
    deriveConfiguredEncoder

  def apply(aggs: ImageAggregations): DisplayImageAggregations =
    DisplayImageAggregations(
      license = displayAggregation(aggs.license, DisplayLicense.apply),
      `source.contributors.agent.label` =
        displayAggregation[AbstractAgent[Minted], DisplayAbstractAgent](
          aggs.sourceContributorAgents,
          DisplayAbstractAgent(_, includesIdentifiers = false)
        ),
      `source.genres.label` = displayAggregation[Genre[Minted], DisplayGenre](
        aggs.sourceGenres,
        DisplayGenre(_, includesIdentifiers = false)
      )
    )

  private def displayAggregation[T, D](
    maybeAgg: Option[Aggregation[T]],
    display: T => D
  ): Option[DisplayAggregation[D]] =
    maybeAgg.map {
      DisplayAggregation(_, display)
    }
}
