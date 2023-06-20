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

    def aggregations: Map[String, Json] =
      json.hcursor
        .downField("aggregations")
        .as[Map[String, Json]]
        .right
        .get
        .filterNot(_._1 == "type")

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

  class HaveAggregationsForMatcher(expectedAggregations: Seq[String])
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

  class HaveAggregationsMatcher(expectedAggregations: Map[String, Seq[Json]])
      extends Matcher[Json] {
    override def apply(left: Json): MatchResult = {
      val differences: Seq[(String, Seq[Json], Seq[Json])] =
        expectedAggregations.collect {
          case (key, buckets) if buckets != left.aggregationBuckets(key) =>
            (key, expectedAggregations(key), buckets)
        }.toSeq

      MatchResult(
        differences.isEmpty,
        s"API Response contained unexpected aggregation buckets: $differences", // TODO: format differences nicely
        s"API Response contained the given aggregations"
      )
    }
  }

  def haveAggregationsFor(expectedAggregations: Seq[String]) =
    new HaveAggregationsForMatcher(expectedAggregations)

  def containProperty(expectedExtension: String) =
    new HaveSomePropertyMatcher(expectedExtension)

  def haveAggregations(expectedAggregations: Map[String, Seq[Json]]) =
    new HaveAggregationsMatcher(
      expectedAggregations
    )

  def onlyHaveAggregations(
    expectedAggregations: Map[String, Seq[Json]]
  ): Matcher[Json] =
    haveAggregationsFor(expectedAggregations.keys.toSeq) and haveAggregations(
      expectedAggregations
    )
}
