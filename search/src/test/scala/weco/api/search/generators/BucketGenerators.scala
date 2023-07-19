package weco.api.search.generators
import io.circe.Json
import io.circe.parser.parse

trait BucketGenerators {
  protected def toKeywordBucket(
                                 dataType: String,
                                 count: Int,
                                 code: String,
                                 label: String
                               ): Json =
    parse(
      s"""
         |{
         |"count": $count,
         |"data": {
         |  "id": "$code",
         |  "label": "$label",
         |  "type": "$dataType"
         |},
         |"type": "AggregationBucket"
         |}""".stripMargin).right.get

  protected def toUnidentifiedBucket(
                                      count: Int,
                                      label: String
                                    ): Json =
    parse(
      s"""
         |{
         |"count": $count,
         |"data": {
         |  "label": "$label"
         |},
         |"type": "AggregationBucket"
         |}""".stripMargin).right.get

}
