package weco.api.search.models.display

import io.circe.Encoder
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.{WorkAggregations, WorkFilter}
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
    filters: Seq[WorkFilter]
  ): DisplayWorkAggregations = {

    val alwaysTrue = Function.const(true) _
    val bucketMatcher = FilterBucketMatcher(filters)

    DisplayWorkAggregations(
      workType = aggs.format.map(
        DisplayAggregation(
          _,
          retainEmpty = bucketMatcher.matchBucket(FormatFilterAgg)
        )
      ),
      `production.dates` = aggs.productionDates
        .map(DisplayAggregation(_, retainEmpty = alwaysTrue)),
      `genres.label` = aggs.genresLabel.map(
        DisplayAggregation(
          _,
          retainEmpty = bucketMatcher.matchBucket(GenreFilterAgg)
        )
      ),
      languages = aggs.languages
        .map(
          DisplayAggregation(
            _,
            retainEmpty = bucketMatcher.matchBucket(LanguagesFilterAgg)
          )
        ),
      `subjects.label` = aggs.subjectsLabel.map(
        DisplayAggregation(
          _,
          retainEmpty = bucketMatcher.matchBucket(SubjectLabelFilterAgg)
        )
      ),
      `contributors.agent.label` = aggs.contributorsAgentsLabel
        .map(
          DisplayAggregation(
            _,
            retainEmpty = bucketMatcher.matchBucket(ContributorsFilterAgg)
          )
        ),
      `items.locations.license` = aggs.itemsLocationsLicense
        .map(
          DisplayAggregation(
            _,
            retainEmpty = bucketMatcher.matchBucket(LicenseFilterAgg)
          )
        ),
      availabilities = aggs.availabilities.map(
        DisplayAggregation(
          _,
          retainEmpty = bucketMatcher.matchBucket(AvailabilitiesFilterAgg)
        )
      )
    )
  }
}
