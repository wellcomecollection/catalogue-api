package weco.api.search.matchers

import io.circe.Json
import org.scalatest.matchers.{MatchResult, Matcher}

trait APIResponseMatchers {
  import scala.language.implicitConversions

  class JsonWithAggregations(json: Json) {
    def aggregationKeys: Seq[String] =
      json.hcursor
        .get[Map[String, Json]]("aggregations")
        .right
        .get
        .keys
        .filterNot(_ == "type") // type is not an aggregation
        .toSeq

    def aggregationBuckets(key: String): Seq[Json] =
      json.hcursor
        .downField("aggregations")
        .downField(key)
        .get[Json]("buckets")
        .right
        .get
        .asArray
        .get
  }

  implicit def _withAggregations(json: Json): JsonWithAggregations =
    new JsonWithAggregations(json)

  class HaveSomePropertyMatcher(expectedProperty: String)
      extends Matcher[Json] {

    def apply(left: Json): MatchResult =
      MatchResult(
        left.asObject.get.contains(expectedProperty),
        s"""Json Object did not contain "$expectedProperty""",
        s"""Json Object contained "$expectedProperty""""
      )
  }

  private def formatStringSeq(strings: Seq[String]): String =
    strings.mkString("['", "','", "']")

  class HaveAggregationsMatcher(expectedAggregations: Seq[String])
      extends Matcher[Json] {
    override def apply(left: Json): MatchResult = {
      val aggregationKeys = left.aggregationKeys
      val leftOnly = aggregationKeys.toSet diff expectedAggregations.toSet
      val rightOnly = expectedAggregations.toSet diff aggregationKeys.toSet
      MatchResult(
        leftOnly.isEmpty && rightOnly.isEmpty,
        s"""API Response expected=${formatStringSeq(expectedAggregations)}, actual=${formatStringSeq(
          aggregationKeys
        )}""",
        s"""API Response contained aggregations: ${formatStringSeq(
          expectedAggregations
        )}"""
      )
    }
  }

  def haveAggregationsFor(expectedAggregations: Seq[String]) =
    new HaveAggregationsMatcher(expectedAggregations)

  def containProperty(expectedExtension: String) =
    new HaveSomePropertyMatcher(expectedExtension)

}
