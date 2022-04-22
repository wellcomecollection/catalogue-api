package weco.api.search.json

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}
import weco.catalogue.display_model.models.{WorkInclude, WorksIncludes}
import weco.catalogue.internal_model.work.generators.{ItemsGenerators, WorkGenerators}

class CatalogueJsonUtilTest extends AnyFunSpec with Matchers with EitherValues with CatalogueJsonUtil with WorkGenerators with ItemsGenerators {
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

    it("includes identifiers") {
      val w = indexedWork()
      val includes = WorksIncludes(WorkInclude.Identifiers)

      val json = w.asJson(includes)

      val expectedJson =
        s"""
           |{
           |  "id" : "${w.id}",
           |  "title" : "${w.data.title.get}",
           |  "identifiers": [
           |    {
           |      "identifierType": {
           |        "id": "${w.sourceIdentifier.identifierType.id}",
           |        "label": "${w.sourceIdentifier.identifierType.label}",
           |        "type": "IdentifierType"
           |      },
           |      "value": "${w.sourceIdentifier.value}",
           |      "type": "Identifier"
           |    }
           |  ],
           |  "alternativeTitles" : [],
           |  "availabilities" : [],
           |  "type" : "Work"
           |}
           |""".stripMargin

      json shouldBe equivalentTo(expectedJson)
    }

    it("includes identifiers on nested objects") {
      val location = createDigitalLocation
      val item = createIdentifiedItemWith(locations = List(location))
      val w = indexedWork().items(List(item))
      val includes = WorksIncludes(WorkInclude.Identifiers, WorkInclude.Items)

      val json = w.asJson(includes)

      val expectedJson =
        s"""
           |{
           |  "id" : "${w.id}",
           |  "title" : "${w.data.title.get}",
           |  "identifiers": [
           |    {
           |      "identifierType": {
           |        "id": "${w.sourceIdentifier.identifierType.id}",
           |        "label": "${w.sourceIdentifier.identifierType.label}",
           |        "type": "IdentifierType"
           |      },
           |      "value": "${w.sourceIdentifier.value}",
           |      "type": "Identifier"
           |    }
           |  ],
           |  "items": [
           |    {
           |      "id": "${item.id.canonicalId}",
           |      "identifiers": [
           |        {
           |          "identifierType": {
           |            "id": "${item.id.sourceIdentifier.identifierType.id}",
           |            "label": "${item.id.sourceIdentifier.identifierType.label}",
           |            "type": "IdentifierType"
           |          },
           |          "value": "${item.id.sourceIdentifier.value}",
           |          "type": "Identifier"
           |        }
           |      ],
           |      "locations" : [
           |        {
           |          "locationType" : {
           |            "id" : "${item.locations.head.locationType.id}",
           |            "label" : "${item.locations.head.locationType.label}",
           |            "type" : "LocationType"
           |          },
           |          "url" : "${location.url}",
           |          ${location.credit.map(c => s""""credit": "$c",""").getOrElse("")}
           |          ${location.linkText.map(c => s""""linkText": "$c",""").getOrElse("")}
           |          "license" : {
           |            "id" : "${location.license.get.id}",
           |            "label" : "${location.license.get.label}",
           |            "url" : "${location.license.get.url}",
           |            "type" : "License"
           |          },
           |          "accessConditions" : [
           |          ],
           |          "type" : "DigitalLocation"
           |        }
           |      ],
           |      "type": "Item"
           |    }
           |  ],
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
