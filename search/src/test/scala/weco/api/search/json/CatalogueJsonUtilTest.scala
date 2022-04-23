package weco.api.search.json

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.display_model.models.{
  DisplaySerialisationTestBase,
  WorkInclude,
  WorksIncludes
}
import weco.catalogue.internal_model.generators.ImageGenerators
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  ProductionEventGenerators,
  SubjectGenerators,
  WorkGenerators
}
import weco.catalogue.internal_model.work._
import java.time.Instant

class CatalogueJsonUtilTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with CatalogueJsonUtil
    with WorkGenerators
    with ItemsGenerators
    with SubjectGenerators
    with ProductionEventGenerators
    with DisplaySerialisationTestBase
    with ImageGenerators {
  describe("WorkOps") {
    it("serialises a work as JSON") {
      val w = indexedWork()
        .format(Format.Books)
        .description(randomAlphanumeric(100))
        .lettering(randomAlphanumeric(100))
        .createdDate(Period("1901", InstantRange(Instant.now, Instant.now)))

      val json = w.asJson(WorksIncludes.none)

      val expectedJson =
        s"""
           |{
           |  "id" : "${w.id}",
           |  "title" : "${w.data.title.get}",
           |  "description": "${w.data.description.get}",
           |  "workType": {
           |    "id": "${Format.Books.id}",
           |    "label": "${Format.Books.label}",
           |    "type": "Format"
           |  },
           |  "lettering": "${w.data.lettering.get}",
           |  "createdDate": {
           |    "label": "1901",
           |    "type": "Period"
           |  },
           |  "alternativeTitles" : [],
           |  "availabilities" : [],
           |  "type" : "Work"
           |}
           |""".stripMargin

      json shouldBe parseObject(expectedJson)
    }

    describe("identifiers") {
      val location = createDigitalLocation
      val item = createIdentifiedItemWith(locations = List(location))
      val w = indexedWork().items(List(item))

      it("includes identifiers with WorkInclude.Identifiers") {
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

        json shouldBe parseObject(expectedJson)
      }

      it("omits identifiers without WorkInclude.Identifiers") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }

      it("includes identifiers on nested objects with WorkInclude.Identifiers") {
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
             |          ${location.credit
               .map(c => s""""credit": "$c",""")
               .getOrElse("")}
             |          ${location.linkText
               .map(c => s""""linkText": "$c",""")
               .getOrElse("")}
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

        json shouldBe parseObject(expectedJson)
      }

      it("omits identifiers on nested objects without WorkInclude.Identifiers") {
        val includes = WorksIncludes(WorkInclude.Items)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "items": [
             |    {
             |      "id": "${item.id.canonicalId}",
             |      "locations" : [
             |        {
             |          "locationType" : {
             |            "id" : "${item.locations.head.locationType.id}",
             |            "label" : "${item.locations.head.locationType.label}",
             |            "type" : "LocationType"
             |          },
             |          "url" : "${location.url}",
             |          ${location.credit
               .map(c => s""""credit": "$c",""")
               .getOrElse("")}
             |          ${location.linkText
               .map(c => s""""linkText": "$c",""")
               .getOrElse("")}
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

        json shouldBe parseObject(expectedJson)
      }
    }

    describe("items") {
      val location = createDigitalLocation
      val item = createIdentifiedItemWith(locations = List(location))
      val w = indexedWork().items(List(item))

      it("includes items with WorkInclude.Items") {
        val includes = WorksIncludes(WorkInclude.Items)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "items": [
             |    {
             |      "id": "${item.id.canonicalId}",
             |      "locations" : [
             |        {
             |          "locationType" : {
             |            "id" : "${item.locations.head.locationType.id}",
             |            "label" : "${item.locations.head.locationType.label}",
             |            "type" : "LocationType"
             |          },
             |          "url" : "${location.url}",
             |          ${location.credit
               .map(c => s""""credit": "$c",""")
               .getOrElse("")}
             |          ${location.linkText
               .map(c => s""""linkText": "$c",""")
               .getOrElse("")}
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

        json shouldBe parseObject(expectedJson)
      }

      it("omits items without WorkInclude.Items") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }

      it("includes an empty list of items with WorkInclude.Items") {
        val w = indexedWork()
        val includes = WorksIncludes(WorkInclude.Items)

        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "items": [],
             |  "alternativeTitles" : [],
             |  "availabilities" : [],
             |  "type" : "Work"
             |}
             |""".stripMargin

        json shouldBe parseObject(expectedJson)
      }
    }

    describe("subjects") {
      val w = indexedWork().subjects(
        (1 to 3).map(_ => createSubject).toList
      )

      it("includes subjects with WorkInclude.Subjects") {
        val includes = WorksIncludes(WorkInclude.Subjects)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "subjects": [${subjects(w.data.subjects)}],
             |  "alternativeTitles" : [],
             |  "availabilities" : [],
             |  "type" : "Work"
             |}
             |""".stripMargin

        json shouldBe parseObject(expectedJson)
      }

      it("omits subjects without WorkInclude.Subjects") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }
    }

    describe("production") {
      val w = indexedWork().production(createProductionEventList(count = 3))

      it("includes production with WorkInclude.Production") {
        val includes = WorksIncludes(WorkInclude.Production)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "production": [${production(w.data.production)}],
             |  "alternativeTitles" : [],
             |  "availabilities" : [],
             |  "type" : "Work"
             |}
             |""".stripMargin

        json shouldBe parseObject(expectedJson)
      }

      it("omits production with WorkInclude.Production") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }
    }

    describe("contributors") {
      val w = indexedWork().contributors(
        List(
          Contributor(agent = Agent(randomAlphanumeric(25)), roles = Nil)
        )
      )

      it("includes contributors with WorkInclude.Contributors") {
        val includes = WorksIncludes(WorkInclude.Contributors)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "contributors": [${contributor(w.data.contributors.head)}],
             |  "alternativeTitles" : [],
             |  "availabilities" : [],
             |  "type" : "Work"
             |}
             |""".stripMargin

        json shouldBe parseObject(expectedJson)
      }

      it("omits contributors with WorkInclude.Contributors") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }
    }

    describe("genres") {
      val w = indexedWork().genres(
        List(
          Genre(
            label = "genre",
            concepts = List(Concept("woodwork"), Concept("etching"))
          )
        )
      )

      it("includes genres with WorkInclude.Genre") {
        val includes = WorksIncludes(WorkInclude.Genres)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "genres": [ ${genres(w.data.genres)} ],
             |  "alternativeTitles" : [],
             |  "availabilities" : [],
             |  "type" : "Work"
             |}
             |""".stripMargin

        json shouldBe parseObject(expectedJson)
      }

      it("omits genres with WorkInclude.Genre") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }
    }

    describe("notes") {
      val w = indexedWork().notes(
        List(
          Note(contents = "A", noteType = NoteType.GeneralNote),
          Note(contents = "B", noteType = NoteType.FundingInformation),
          Note(contents = "C", noteType = NoteType.GeneralNote)
        )
      )

      it("includes notes with WorkInclude.Notes") {
        val includes = WorksIncludes(WorkInclude.Notes)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "notes": [
             |    {
             |      "noteType": {
             |        "id": "general-note",
             |        "label": "Notes",
             |        "type": "NoteType"
             |      },
             |      "contents": ["A", "C"],
             |      "type": "Note"
             |    },
             |    {
             |      "noteType": {
             |        "id": "funding-info",
             |        "label": "Funding information",
             |        "type": "NoteType"
             |      },
             |      "contents": ["B"],
             |      "type": "Note"
             |    }
             |  ],
             |  "alternativeTitles": [],
             |  "availabilities" : [],
             |  "type" : "Work"
             |}
             |""".stripMargin

        json shouldBe parseObject(expectedJson)
      }

      it("omits notes with WorkInclude.Notes") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }
    }

    describe("images") {
      val w = indexedWork().imageData(
        (1 to 3).map(_ => createImageData.toIdentified).toList
      )

      it("includes images with WorkInclude.Images") {
        val includes = WorksIncludes(WorkInclude.Images)
        val json = w.asJson(includes)

        val expectedJson =
          s"""
             |{
             |  "id" : "${w.id}",
             |  "title" : "${w.data.title.get}",
             |  "images": [${workImageIncludes(w.data.imageData)}],
             |  "alternativeTitles": [],
             |  "availabilities" : [],
             |  "type" : "Work"
             |}
             |""".stripMargin

        json shouldBe parseObject(expectedJson)
      }

      it("omits images with WorkInclude.Images") {
        val includes = WorksIncludes()
        val json = w.asJson(includes)

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

        json shouldBe parseObject(expectedJson)
      }
    }
  }

  def parseJson(s: String): Json =
    parse(s.stripMargin).right.value

  def parseObject(s: String): Json = {
    val j = parseJson(s)
    j.isObject shouldBe true
    j
  }
}
