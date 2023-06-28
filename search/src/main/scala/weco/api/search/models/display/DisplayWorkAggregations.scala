package weco.api.search.models.display

import io.circe.{Encoder, Json}
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.{
  FormatFilter,
  LanguagesFilter,
  WorkAggregations,
  WorkFilter
}
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

trait FilterAggregationMatcher {
  def matches(bucketData: Json): Boolean

}

class aggregationDataLabelInFilter(labels: Seq[String])
    extends FilterAggregationMatcher {
  def matches(bucketData: Json): Boolean =
    bucketData.hcursor.get[String]("label") match {
      case Right(value) => labels.contains(value)
      case _            => false
    }
}

class aggregationDataIdInFilter(labels: Seq[String])
    extends FilterAggregationMatcher {
  def matches(bucketData: Json): Boolean =
    bucketData.hcursor.get[String]("id") match {
      case Right(value) => labels.contains(value)
      case _            => false
    }
}

object DisplayWorkAggregations {
  import scala.language.implicitConversions

  implicit def encoder: Encoder[DisplayWorkAggregations] =
    deriveConfiguredEncoder

  implicit def _workTypeFilterMatcher(
    filters: Seq[FormatFilter]
  ): FilterAggregationMatcher =
    new aggregationDataIdInFilter(filters.flatMap(_.formatIds))

  implicit def _languagesFilterMatcher(
    filters: Seq[LanguagesFilter]
  ): FilterAggregationMatcher =
    new aggregationDataIdInFilter(filters.flatMap(_.languageIds))

  def apply(
    aggs: WorkAggregations,
    filters: Seq[WorkFilter]
  ): DisplayWorkAggregations = {

    val alwaysTrue = Function.const(true) _

    DisplayWorkAggregations(
      workType =
        aggs.format.map(DisplayAggregation(_, retainEmpty = filters.collect {
          case f: FormatFilter => f
        }.matches)),
      `production.dates` = aggs.productionDates
        .map(DisplayAggregation(_, retainEmpty = alwaysTrue)),
      `genres.label` =
        aggs.genresLabel.map(DisplayAggregation(_, retainEmpty = alwaysTrue)),
      languages = aggs.languages
        .map(DisplayAggregation(_, retainEmpty = filters.collect {
          case f: LanguagesFilter => f
        }.matches)),
      `subjects.label` =
        aggs.subjectsLabel.map(DisplayAggregation(_, retainEmpty = alwaysTrue)),
      `contributors.agent.label` = aggs.contributorsAgentsLabel
        .map(DisplayAggregation(_, retainEmpty = alwaysTrue)),
      `items.locations.license` = aggs.itemsLocationsLicense
        .map(DisplayAggregation(_, retainEmpty = alwaysTrue)),
      availabilities =
        aggs.availabilities.map(DisplayAggregation(_, retainEmpty = alwaysTrue))
    )
  }
}
