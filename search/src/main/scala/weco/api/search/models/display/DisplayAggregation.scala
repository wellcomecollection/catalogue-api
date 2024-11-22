package weco.api.search.models.display

import io.circe.generic.extras.JsonKey
import weco.api.search.models.{
  Aggregation,
  AggregationBucket,
  AggregationBucketData
}

case class DisplayAggregation(
  buckets: List[DisplayAggregationBucket],
  @JsonKey("type") ontologyType: String = "Aggregation"
)

case object DisplayAggregation {

  def apply(
    agg: Aggregation,
    retainEmpty: AggregationBucket => Boolean
  ): DisplayAggregation =
    DisplayAggregation(
      buckets = agg.buckets.collect {
        case bucket if bucket.count > 0 || retainEmpty(bucket) =>
          DisplayAggregationBucket(
            data = bucket.data,
            count = bucket.count
          )
      }
    )
}

case class DisplayAggregationBucket(
  data: AggregationBucketData,
  count: Int,
  @JsonKey("type") ontologyType: String = "AggregationBucket"
)
