package uk.ac.wellcome.platform.api.models

import io.circe.generic.extras.semiauto._
import io.circe.Encoder
import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import weco.catalogue.internal_model.identifiers.IdState.Minted
import weco.catalogue.internal_model.work.{Contributor, Genre, Subject}

@Schema(
  name = "Aggregations",
  description = "A map containing the requested aggregations."
)
case class DisplayWorkAggregations(
  @Schema(
    description = "Format aggregation on a set of results."
  ) workType: Option[DisplayAggregation[DisplayFormat]],
  @Schema(
    description = "Date aggregation on a set of results."
  ) @JsonKey("production.dates") productionDates: Option[
    DisplayAggregation[DisplayPeriod]],
  @Schema(
    description = "Genre aggregation on a set of results."
  ) `genres.label`: Option[DisplayAggregation[DisplayGenre]],
  @Schema(
    description = "Subject aggregation on a set of results."
  ) `subjects.label`: Option[DisplayAggregation[DisplaySubject]],
  @Schema(
    description = "Contributor aggregation on a set of results."
  ) `contributors.agent.label`: Option[
    DisplayAggregation[DisplayAbstractAgent]],
  @Schema(
    description = "Language aggregation on a set of results."
  ) languages: Option[DisplayAggregation[DisplayLanguage]],
  @Schema(
    description = "License aggregation on a set of results."
  ) `items.locations.license`: Option[DisplayAggregation[DisplayLicense]],
  @Schema(
    description = "Availabilities aggregation on a set of results."
  ) availabilities: Option[DisplayAggregation[DisplayAvailability]],
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "Aggregations")

object DisplayWorkAggregations {

  implicit def encoder: Encoder[DisplayWorkAggregations] =
    deriveConfiguredEncoder

  def apply(
    aggs: WorkAggregations,
    aggregationRequests: Seq[WorkAggregationRequest]): DisplayWorkAggregations =
    DisplayWorkAggregations(
      workType = displayAggregation(aggs.format, DisplayFormat.apply),
      productionDates =
        displayAggregation(aggs.productionDates, DisplayPeriod.apply),
      `genres.label` =
        whenRequestPresent(aggregationRequests, WorkAggregationRequest.Genre)(
          displayAggregation[Genre[Minted], DisplayGenre](
            aggs.genresLabel,
            DisplayGenre(_, includesIdentifiers = false))
        ),
      languages = displayAggregation(aggs.languages, DisplayLanguage.apply),
      `subjects.label` =
        whenRequestPresent(aggregationRequests, WorkAggregationRequest.Subject)(
          displayAggregation[Subject[Minted], DisplaySubject](
            aggs.subjectsLabel,
            subject => DisplaySubject(subject, includesIdentifiers = false)
          )),
      `contributors.agent.label` = whenRequestPresent(
        aggregationRequests,
        WorkAggregationRequest.Contributor)(
        displayAggregation[Contributor[Minted], DisplayAbstractAgent](
          aggs.contributorsAgentsLabel,
          contributor =>
            DisplayAbstractAgent(contributor.agent, includesIdentifiers = false)
        )),
      `items.locations.license` =
        whenRequestPresent(aggregationRequests, WorkAggregationRequest.License)(
          displayAggregation(aggs.itemsLocationsLicense, DisplayLicense.apply)
        ),
      availabilities =
        displayAggregation(aggs.availabilities, DisplayAvailability.apply)
    )

  def whenRequestPresent[T](
    requests: Seq[WorkAggregationRequest],
    conditionalRequest: WorkAggregationRequest
  )(property: Option[T]): Option[T] =
    if (requests.contains(conditionalRequest)) {
      property
    } else {
      None
    }

  private def displayAggregation[T, D](
    maybeAgg: Option[Aggregation[T]],
    display: T => D): Option[DisplayAggregation[D]] =
    maybeAgg.map {
      DisplayAggregation(_, display)
    }
}
