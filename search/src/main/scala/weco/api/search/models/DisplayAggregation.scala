package weco.api.search.models

import io.circe.generic.extras.JsonKey

case class DisplayAggregation[T](
  buckets: List[DisplayAggregationBucket[T]],
  @JsonKey("type") ontologyType: String = "Aggregation"
)

case object DisplayAggregation {
  def apply[T, DisplayT](
    agg: Aggregation[T],
    display: T => DisplayT
  ): DisplayAggregation[DisplayT] =
    DisplayAggregation(
      buckets = agg.buckets.map { bucket =>
        DisplayAggregationBucket(
          data = display(bucket.data),
          count = bucket.count
        )
      }
    )
}

case class DisplayAggregationBucket[T](
  data: T,
  count: Int,
  @JsonKey("type") ontologyType: String = "AggregationBucket"
)
