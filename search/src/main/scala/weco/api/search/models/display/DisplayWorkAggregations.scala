package weco.api.search.models.display

import io.circe.{Encoder, Json}
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.{
  AvailabilitiesFilter,
  ContributorsFilter,
  FormatFilter,
  GenreFilter,
  LanguagesFilter,
  LicenseFilter,
  SubjectLabelFilter,
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
class neverAggregationMatcher() extends FilterAggregationMatcher {
  def matches(bucketData: Json): Boolean =
    false
}
class multiAggregationMatcher(matchers: Seq[FilterAggregationMatcher])
    extends FilterAggregationMatcher {
  def matches(bucketData: Json): Boolean =
    matchers.exists(_.matches(bucketData))
}

object FilterBucketMatcher {
  import scala.reflect.ClassTag
  def apply[T: ClassTag](
    filters: Seq[WorkFilter]
  ): FilterAggregationMatcher =
    new multiAggregationMatcher(filters.collect {
      case filter: T =>
        filter match {
          case FormatFilter(ids)    => new aggregationDataIdInFilter(ids)
          case LanguagesFilter(ids) => new aggregationDataIdInFilter(ids)
          case GenreFilter(labels)  => new aggregationDataLabelInFilter(labels)
          case SubjectLabelFilter(labels) =>
            new aggregationDataLabelInFilter(labels)
          case ContributorsFilter(labels) =>
            new aggregationDataLabelInFilter(labels)
          case LicenseFilter(ids) => new aggregationDataIdInFilter(ids)
          case AvailabilitiesFilter(availabilityIds) =>
            new aggregationDataIdInFilter(availabilityIds)
          case _ => new neverAggregationMatcher
        }
    })

}

object DisplayWorkAggregations {

  implicit def encoder: Encoder[DisplayWorkAggregations] =
    deriveConfiguredEncoder

  def apply(
    aggs: WorkAggregations,
    filters: Seq[WorkFilter]
  ): DisplayWorkAggregations = {

    val alwaysTrue = Function.const(true) _

    DisplayWorkAggregations(
      workType = aggs.format
        .map(
          DisplayAggregation(
            _,
            retainEmpty = FilterBucketMatcher[FormatFilter](filters).matches
          )
        ),
      `production.dates` = aggs.productionDates
        .map(DisplayAggregation(_, retainEmpty = alwaysTrue)),
      `genres.label` = aggs.genresLabel.map(
        DisplayAggregation(
          _,
          retainEmpty = FilterBucketMatcher[GenreFilter](filters).matches
        )
      ),
      languages = aggs.languages
        .map(
          DisplayAggregation(
            _,
            retainEmpty = FilterBucketMatcher[LanguagesFilter](filters).matches
          )
        ),
      `subjects.label` = aggs.subjectsLabel.map(
        DisplayAggregation(
          _,
          retainEmpty = FilterBucketMatcher[SubjectLabelFilter](filters).matches
        )
      ),
      `contributors.agent.label` = aggs.contributorsAgentsLabel
        .map(
          DisplayAggregation(
            _,
            retainEmpty =
              FilterBucketMatcher[ContributorsFilter](filters).matches
          )
        ),
      `items.locations.license` = aggs.itemsLocationsLicense
        .map(
          DisplayAggregation(
            _,
            retainEmpty = FilterBucketMatcher[LicenseFilter](filters).matches
          )
        ),
      availabilities = aggs.availabilities.map(
        DisplayAggregation(
          _,
          retainEmpty =
            FilterBucketMatcher[AvailabilitiesFilter](filters).matches
        )
      )
    )
  }
}
