package weco.api.search.models.display

import io.circe.Encoder
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import weco.api.search.models.{ImageAggregations, ImageFilter}
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

  def apply(
    aggs: ImageAggregations,
    filters: Seq[ImageFilter]
  ): DisplayImageAggregations = {
    val bucketMatcher = FilterBucketMatcher(filters)

    DisplayImageAggregations(
      license = aggs.license.map(
        DisplayAggregation(
          _,
          retainEmpty = bucketMatcher.matchBucket(LicenseFilterAgg)
        )
      ),
      `source.contributors.agent.label` = aggs.sourceContributorAgents
        .map(
          DisplayAggregation(
            _,
            retainEmpty = bucketMatcher.matchBucket(ContributorsFilterAgg)
          )
        ),
      `source.genres.label` = aggs.sourceGenres.map(
        DisplayAggregation(
          _,
          retainEmpty = bucketMatcher.matchBucket(GenreFilterAgg)
        )
      ),
      `source.subjects.label` = aggs.sourceSubjects.map(
        DisplayAggregation(
          _,
          retainEmpty = bucketMatcher.matchBucket(SubjectLabelFilterAgg)
        )
      )
    )
  }
}
