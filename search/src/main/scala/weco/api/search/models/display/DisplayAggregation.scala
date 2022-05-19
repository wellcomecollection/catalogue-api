package weco.api.search.models.display

import io.circe.Json
import io.circe.generic.extras.JsonKey
import weco.api.search.models.Aggregation

case class DisplayAggregation(
  buckets: List[DisplayAggregationBucket[Json]],
  @JsonKey("type") ontologyType: String = "Aggregation"
)

case object DisplayAggregation {
  def apply(agg: Aggregation[Json]): DisplayAggregation =
    DisplayAggregation(
      buckets = agg.buckets.map { bucket =>
        DisplayAggregationBucket(
          data = bucket.data,
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
