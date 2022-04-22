package weco.api.search.json

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}
import weco.catalogue.display_model.models.WorksIncludes
import weco.catalogue.internal_model.work.generators.WorkGenerators

class CatalogueJsonUtilTest extends AnyFunSpec with Matchers with EitherValues with CatalogueJsonUtil with WorkGenerators {
  describe("WorkOps") {
    it("serialises a work as JSON") {
      val w = indexedWork()

      val json = w.asJson(WorksIncludes.none)

      val expectedJson =
        s"""
           |{
           |  "id" : "${w.id}",
           |  "title" : "${w.data.title.get}",
           |  "alternativeTitles" : [],
           |  "availabilities" : [],
           |  "type" : "Work"
           |}
           |""".stripMargin

      json shouldBe equivalentTo(expectedJson)
    }
  }

  class JsonMatcher(right: String) extends BeMatcher[Json] {
    def apply(left: Json): MatchResult =
      MatchResult(
        left.deepDropNullValues == parseObject(right).deepDropNullValues,
        s"$left is not equivalent to $right",
        s"$left is equivalent to $right",
      )
  }

  def equivalentTo(right: String) =
    new JsonMatcher(right)

  def parseJson(s: String): Json =
    parse(s.stripMargin).right.value

  def parseObject(s: String): Json = {
    val j = parseJson(s)
    j.isObject shouldBe true
    j
  }
}
