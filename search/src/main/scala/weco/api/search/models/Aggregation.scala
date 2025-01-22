package weco.api.search.models

import io.circe.generic.extras.JsonKey
import io.circe.{Json}
import io.circe.parser._
import io.circe.optics.JsonPath._

import scala.util.{Try}

// Each aggregated field is associated with two aggregations - a 'filtered' aggregation and a 'global' aggregation.
//
// The 'filtered' aggregation has the following structure (the 'nestedSelf' bucket
// is only included for aggregations with a paired filter):
//{
//  "filtered": {
//    "nested": {
//      "terms": {
//        "buckets": [...]
//      }
//    },
//    "nestedSelf": {
//     "terms": {
//        "buckets": [...]
//      }
//    }
//  }
//}
//
// The 'global' aggregation has the following structure:
//{
//  "nestedSelf": {
//    "terms": {
//      "buckets": [...]
//    }
//  }
//}
//
// Each bucket has the following format (the nested buckets are only included in 'labelled ID' aggregations):
//{
//  "key": "i",
//  "doc_count": 1,
//  "labels": {
//    "buckets": [
//      {
//        "key": "Audio",
//        "doc_count": 1
//      }
//    ]
//  }
//}
// For more information about why aggregations are structured this way, see the AggregationsBuilder class
object AggregationMapping {
  import weco.json.JsonUtil._

  // Optics for retrieving buckets from all three possible locations
  private val globalAggBuckets =
    root.filtered.nested.terms.buckets.each.as[RawAggregationBucket]
  private val selfAggBuckets =
    root.filtered.nestedSelf.terms.buckets.each.as[RawAggregationBucket]
  private val unfilteredSelfAggBuckets =
    root.nestedSelf.terms.buckets.each.as[RawAggregationBucket]

  // Retrieve all 'nested' buckets, followed by all 'nestedSelf' buckets. The self buckets exist only to "top up"
  // the main list with the filtered values if those values were not returned in the main ('nested') aggregation.
  // If any of the filtered terms are present in the main aggregation, then they will be duplicated
  // in the self buckets, hence the need for distinct.
  private def getAllFilteredBuckets(json: Json): Seq[RawAggregationBucket] =
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

  // Create a map of IDs to labels for all values included in the paired filter. This is to cover the special case of
  // a filtered 'nestedSelf' aggregation bucket returning an item count of 0. When this happens, there is no way for the
  // filtered aggregation to map IDs to labels, so we use this mapping to fill in the gaps.
  private def getUnfilteredIdLabelMap(json: Json): Map[String, String] = {
    val unfilteredSelfBuckets = parseNestedAggregationBuckets(
      unfilteredSelfAggBuckets.getAll(json))
    unfilteredSelfBuckets
      .map(bucket => bucket.data.id -> bucket.data.label)
      .toMap
  }

  def aggregationParser(
    filteredJsonString: String,
    globalJsonString: String
  ): Try[Aggregation] = {
    val unfilteredIdLabelMap = parse(globalJsonString)
      .map { json =>
        getUnfilteredIdLabelMap(json)
      }
      .getOrElse(Map())

    parse(filteredJsonString)
      .map { json =>
        val nestedBuckets =
          parseNestedAggregationBuckets(getAllFilteredBuckets(json))

        val nestedBucketsWithUpdatedLabels = nestedBuckets.map { bucket =>
          val id = bucket.data.id
          bucket.copy(
            data = AggregationBucketData(
              id = id,
              unfilteredIdLabelMap.getOrElse(id, bucket.data.label)))
        }
        Aggregation(nestedBucketsWithUpdatedLabels)
      }
  }.toTry
}

case class Aggregation(buckets: List[AggregationBucket])
case class AggregationBucket(data: AggregationBucketData, count: Int)
case class AggregationBucketData(id: String, label: String)
