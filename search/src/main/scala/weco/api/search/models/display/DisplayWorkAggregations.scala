package weco.api.search.models.display

import io.circe.Encoder
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.request.WorkAggregationRequest
import weco.api.search.models.{Aggregation, WorkAggregations}
import weco.catalogue.display_model.languages.DisplayLanguage
import weco.catalogue.display_model.locations.DisplayLicense
import weco.catalogue.display_model.work._
import weco.catalogue.internal_model.identifiers.IdState.Minted
import weco.catalogue.internal_model.work.{Contributor, Genre, Subject}
import weco.http.json.DisplayJsonUtil._

case class DisplayWorkAggregations(
  workType: Option[DisplayAggregation[DisplayFormat]],
  @JsonKey("production.dates") `production.dates`: Option[
    DisplayAggregation[DisplayPeriod]
  ],
  `genres.label`: Option[DisplayAggregation[DisplayGenre]],
  `subjects.label`: Option[DisplayAggregation[DisplaySubject]],
  `contributors.agent.label`: Option[DisplayAggregation[DisplayAbstractAgent]],
  languages: Option[DisplayAggregation[DisplayLanguage]],
  `items.locations.license`: Option[DisplayAggregation[DisplayLicense]],
  availabilities: Option[DisplayAggregation[DisplayAvailability]],
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
      workType = displayAggregation(aggs.format, DisplayFormat.apply),
      `production.dates` =
        displayAggregation(aggs.productionDates, DisplayPeriod.apply),
      `genres.label` =
        whenRequestPresent(aggregationRequests, WorkAggregationRequest.Genre)(
          displayAggregation[Genre[Minted], DisplayGenre](
            aggs.genresLabel,
            DisplayGenre(_, includesIdentifiers = false)
          )
        ),
      languages = displayAggregation(aggs.languages, DisplayLanguage.apply),
      `subjects.label` =
        whenRequestPresent(aggregationRequests, WorkAggregationRequest.Subject)(
          displayAggregation[Subject[Minted], DisplaySubject](
            aggs.subjectsLabel,
            subject => DisplaySubject(subject, includesIdentifiers = false)
          )
        ),
      `contributors.agent.label` = whenRequestPresent(
        aggregationRequests,
        WorkAggregationRequest.Contributor
      )(
        displayAggregation[Contributor[Minted], DisplayAbstractAgent](
          aggs.contributorsAgentsLabel,
          contributor =>
            DisplayAbstractAgent(contributor.agent, includesIdentifiers = false)
        )
      ),
      `items.locations.license` =
        whenRequestPresent(aggregationRequests, WorkAggregationRequest.License)(
          displayAggregation(aggs.itemsLocationsLicense, DisplayLicense.apply)
        ),
      availabilities =
        displayAggregation(aggs.availabilities, DisplayAvailability.apply)
    )

  private def whenRequestPresent[T](
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
    display: T => D
  ): Option[DisplayAggregation[D]] =
    maybeAgg.map {
      DisplayAggregation(_, display)
    }
}
