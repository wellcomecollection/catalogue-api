package weco.api.search.generators
import io.circe.Json
import io.circe.parser.parse

trait BucketGenerators {
  protected def toKeywordBucket(
                                 count: Int,
                                 id: String,
                                 label: String
                               ): Json =
    parse(
      s"""
         |{
         |"count": $count,
         |"data": {
         |  "id": "$id",
         |  "label": "$label"
         |},
         |"type": "AggregationBucket"
         |}""".stripMargin).right.get

  protected def toKeywordBucket(count: Int, id: String): Json =
    toKeywordBucket(count, id, id)
}
