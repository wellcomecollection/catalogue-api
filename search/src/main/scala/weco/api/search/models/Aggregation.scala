package weco.api.search.models

import io.circe.generic.extras.JsonKey
import io.circe.{Error, Json}
import io.circe.parser._
import io.circe.optics.JsonPath._

import scala.util.{Success, Try}

// This object maps an aggregation result from Elasticsearch into our
// Aggregation data type.  The results from ES are of the form:
//
//    {
//      "buckets": [
//        {
//          "key": "chi",
//          "doc_count": 4,
//          "filtered": {
//            "doc_count": 3
//          }
//        },
//        ...
//      ],
//      ...
//    }
//
// If the buckets have a subaggregation named "filtered", then we use the
// count from there; otherwise we use the count from the root of the bucket.
//
// Some results will come wrapped in a global aggregation, they will have the
// above form inside an unknown key:
//
//    {
//      "doc_count": 123,
//      "some_name": {
//        "buckets": [
//          {
//            "key": "chi",
//            "doc_count": 4,
//            "filtered": {
//              "doc_count": 3
//            }
//          },
//        ...
//        ],
//        ...
//      },
//      ...
//    }
object AggregationMapping {
  import weco.json.JsonUtil._
  private case class Result(buckets: Option[Seq[Bucket]])

  // We can't predict the key name of the resultant sub-aggregation.
  // This optic says, "for each key of the root object that has a key `buckets`, decode
  // the value of that field as an array of Buckets"
  private val globalAggBuckets = root.each.buckets.each.as[Bucket]
  // This optic does the same for buckets within the self aggregation
  private val selfAggBuckets = root.self.each.buckets.each.as[Bucket]

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
  private def bucketsFromAnywhere(json: Json): Seq[Bucket] =
    (globalAggBuckets.getAll(json) ++ selfAggBuckets.getAll(json)) distinct

  private case class Bucket(
    key: Json,
    @JsonKey("doc_count") count: Int,
    filtered: Option[FilteredResult]
  ) {
    def docCount: Int =
      filtered match {
        case Some(f) => f.count
        case None    => count
      }
  }

  private case class FilteredResult(@JsonKey("doc_count") count: Int)

  trait AggregationDecoder[T] {
    def apply(json: Json): Either[Error, T]
  }

  object AggregationDecoder {
    implicit val jsonAggregationDecoder: AggregationDecoder[Json] =
      (json: Json) => json.as[String].flatMap(parse)
    implicit val stringAggregationDecoder: AggregationDecoder[String] =
      (json: Json) => json.as[String]
  }

  def aggregationParser[T: AggregationDecoder](
    jsonString: String
  )(implicit decoder: AggregationDecoder[T]): Try[Aggregation[T]] =
    fromJson[Result](jsonString)
      .flatMap {
        case Result(Some(buckets)) =>
          Success(buckets)
        case Result(None) =>
          parse(jsonString)
            .map(bucketsFromAnywhere)
            .toTry
      }
      .map { buckets =>
        buckets.map { b =>
          (decoder(b.key), b.docCount)
        }
      }
      .map { tally =>
        tally.collect {
          case (Right(t), count) =>
            AggregationBucket(t, count = count)
        }
      }
      .map { buckets =>
        Aggregation(
          buckets.toList
        )
      }
}

case class Aggregation[+T](buckets: List[AggregationBucket[T]])
case class AggregationBucket[+T](data: T, count: Int)
