package weco.api.search.models.display

import io.circe.Json
import weco.api.search.models.{
  AvailabilitiesFilter,
  ContributorsFilter,
  FormatFilter,
  GenreFilter,
  LanguagesFilter,
  LicenseFilter,
  SubjectLabelFilter,
  WorkFilter
}

trait FilterAggregationMatcher {
  def matchBucket(bucketData: Json): Boolean
}

case class AggregationDataLabelInFilter(labels: Seq[String])
    extends FilterAggregationMatcher {
  def matchBucket(bucketData: Json): Boolean =
    bucketData.hcursor.get[String]("label") match {
      case Right(value) => labels.contains(value)
      case _            => false
    }
}

case class AggregationDataIdInFilter(labels: Seq[String])
    extends FilterAggregationMatcher {
  def matchBucket(bucketData: Json): Boolean =
    bucketData.hcursor.get[String]("id") match {
      case Right(value) => labels.contains(value)
      case _            => false
    }
}

case class NeverAggregationMatcher() extends FilterAggregationMatcher {
  def matchBucket(bucketData: Json): Boolean =
    false
}

sealed trait FilterWithMatchingAggregation
case object FormatFilterAgg extends FilterWithMatchingAggregation
case object LanguagesFilterAgg extends FilterWithMatchingAggregation
case object GenreFilterAgg extends FilterWithMatchingAggregation
case object SubjectLabelFilterAgg extends FilterWithMatchingAggregation
case object ContributorsFilterAgg extends FilterWithMatchingAggregation
case object LicenseFilterAgg extends FilterWithMatchingAggregation
case object AvailabilitiesFilterAgg extends FilterWithMatchingAggregation

class FilterBucketMatcher(
  filters: Map[FilterWithMatchingAggregation, FilterAggregationMatcher]
) {

  def matchBucket(
    aggregationType: FilterWithMatchingAggregation
  )(bucketData: Json): Boolean =
    filters
      .getOrElse(aggregationType, NeverAggregationMatcher())
      .matchBucket(bucketData)
}

object FilterBucketMatcher {
  def apply(filters: Seq[WorkFilter]) =
    new FilterBucketMatcher(
      filters collect {
        case FormatFilter(ids) =>
          FormatFilterAgg -> AggregationDataIdInFilter(ids)
        case LanguagesFilter(ids) =>
          LanguagesFilterAgg -> AggregationDataIdInFilter(ids)
        case GenreFilter(labels) =>
          GenreFilterAgg -> AggregationDataLabelInFilter(labels)
        case SubjectLabelFilter(labels) =>
          SubjectLabelFilterAgg ->
            AggregationDataLabelInFilter(labels)
        case ContributorsFilter(labels) =>
          ContributorsFilterAgg ->
            AggregationDataLabelInFilter(labels)
        case LicenseFilter(ids) =>
          LicenseFilterAgg -> AggregationDataIdInFilter(ids)
        case AvailabilitiesFilter(availabilityIds) =>
          AvailabilitiesFilterAgg ->
            AggregationDataIdInFilter(availabilityIds)
      } toMap
    )

}
