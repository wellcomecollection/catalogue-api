package weco.api.search.models.display

import weco.api.search.models.{AggregationBucket, AvailabilitiesFilter, ContributorsLabelFilter, DocumentFilter, FormatFilter, GenreLabelFilter, LanguagesFilter, LicenseFilter, SubjectLabelFilter}

trait FilterAggregationMatcher {
  def matchBucket(bucketData: AggregationBucket): Boolean
}

case class AggregationDataLabelInFilter(labels: Seq[String])
    extends FilterAggregationMatcher {
  def matchBucket(bucket: AggregationBucket): Boolean =
    labels.contains(bucket.data.label)
}

case class AggregationDataIdInFilter(labels: Seq[String])
    extends FilterAggregationMatcher {
  def matchBucket(bucket: AggregationBucket): Boolean =
    labels.contains(bucket.data.id)

}

case class NeverAggregationMatcher() extends FilterAggregationMatcher {
  def matchBucket(bucket: AggregationBucket): Boolean =
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
  )(bucket: AggregationBucket): Boolean =
    filters
      .getOrElse(aggregationType, NeverAggregationMatcher())
      .matchBucket(bucket)
}

object FilterBucketMatcher {
  def apply(filters: Seq[DocumentFilter]) =
    new FilterBucketMatcher(
      filters collect {
        case FormatFilter(ids) =>
          FormatFilterAgg -> AggregationDataIdInFilter(ids)
        case LanguagesFilter(ids) =>
          LanguagesFilterAgg -> AggregationDataIdInFilter(ids)
        case GenreLabelFilter(labels) =>
          GenreFilterAgg -> AggregationDataLabelInFilter(labels)
        case SubjectLabelFilter(labels) =>
          SubjectLabelFilterAgg ->
            AggregationDataLabelInFilter(labels)
        case ContributorsLabelFilter(labels) =>
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
