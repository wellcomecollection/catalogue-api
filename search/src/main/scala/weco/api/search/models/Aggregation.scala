package weco.api.search.models

import io.circe.generic.extras.JsonKey
import io.circe.{Json}
import io.circe.parser._
import io.circe.optics.JsonPath._

import scala.util.{Try}

object AggregationMapping {
  import weco.json.JsonUtil._
  // We can't predict the key name of the resultant sub-aggregation.
  // This optic says, "for each key of the root object that has a key `buckets`, decode
  // the value of that field as an array of Buckets"
  private val globalAggBuckets =
    root.nested.each.buckets.each.as[RawAggregationBucket]
  // This optic does the same for buckets within the self aggregation
  private val selfAggBuckets =
    root.nestedSelf.each.buckets.each.as[RawAggregationBucket]

  // When we use the self aggregation pattern, buckets are returned
  // in aggregations at multiple depths. This will return
  // buckets from the expected locations.
  // The order of sub aggregations vs the top-level aggregation is not guaranteed,
  // so construct a sequence consisting of first the top-level buckets, then the self buckets.
  // The top-level buckets will contain all the properly counted bucket values.  The self buckets
  // exist only to "top up" the main list with the filtered values if those values were not returned in
  // the main aggregation.
  // If any of the filtered terms are present in the main aggregation, then they will be duplicated
  // in the self buckets, hence the need for distinct.
  private def bucketsFromAnywhere(json: Json): Seq[RawAggregationBucket] =
    (globalAggBuckets.getAll(json) ++ selfAggBuckets.getAll(json)) distinct

  private case class LabelBucket(
    key: String,
    doc_count: Int
  )

  private case class RawAggregationBucket(
    key: Json,
    @JsonKey("labels") labelSubAggregation: Option[Json],
    @JsonKey("doc_count") count: Int
  )

  private def parseNestedAggregationBuckets(
    buckets: Seq[RawAggregationBucket]
  ) =
    buckets
      .map { bucket =>
        // Each ID-based aggregation bucket contains a list of label-based sub-aggregation buckets,
        // storing a list of labels associated with a given ID.
        val labelBucketsOption = bucket.labelSubAggregation
          .map(
            _.hcursor.downField("buckets").as[Seq[LabelBucket]]
          )

        // Retrieve the label from the first bucket. There might be multiple labels associated with a given ID,
        // but we only want to expose the most commonly used one to the frontend.
        val firstLabelBucket: Option[LabelBucket] = for {
          decoderResult <- labelBucketsOption
          labelBuckets <- decoderResult.toOption
          firstBucket <- labelBuckets.headOption
        } yield firstBucket

        val key = bucket.key.as[String].toOption.get

        // For label-based aggregations (which do not contain sub-aggregation buckets), set the label equal to the key.
        val label = firstLabelBucket.map(_.key).getOrElse(key)

        AggregationBucket(
          data = AggregationBucketData(id = key, label),
          count = bucket.count
        )
      }
      .toList
      .sortBy(b => (-b.count, b.data.label))

  def aggregationParser(
    jsonString: String
  ): Try[Aggregation] = {
    parse(jsonString)
      .map { json =>
        val nestedBuckets =
          parseNestedAggregationBuckets(bucketsFromAnywhere(json))
        Aggregation(nestedBuckets)
      }
  }.toTry
}

case class Aggregation(buckets: List[AggregationBucket])
case class AggregationBucket(data: AggregationBucketData, count: Int)
case class AggregationBucketData(id: String, label: String)
