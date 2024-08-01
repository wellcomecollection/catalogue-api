package weco.api.items

import org.apache.pekko.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.items.fixtures.{ItemsApiFixture, ItemsApiGenerators}
import weco.catalogue.display_model.generators.IdentifiersGenerators
import weco.fixtures.LocalResources
import weco.json.utils.JsonAssertions
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraItemNumber

class ItemsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with ItemsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with IdentifiersGenerators
    with ItemsApiGenerators
    with SierraIdentifierGenerators
    with LocalResources {

  describe("look up the status of an item") {
    it("shows a user the items on a work") {
      val resourceName = "work-with-temporarily-unavailable-item.json"
      val workId = "eccsqg7j"
      val itemId = "otdlfo0u"
      val sierraItemNumber = SierraItemNumber("1024083")

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(workId),
          catalogueWorkResponse(resourceName)
        )
      )

      val sierraResponses = Seq(
        (
          sierraItemRequest(sierraItemNumber),
          HttpResponse(
            entity = sierraItemResponse(
              sierraItemNumber = sierraItemNumber
            )
          )
        )
      )

      val contentApiVenueResponses =
        Seq((contentApiVenueRequest("library"), contentApiVenueResponse()))

      withItemsApi(
        catalogueResponses,
        sierraResponses,
        contentApiVenueResponses
      ) { _ =>
        val path = s"/works/$workId"

        val expectedJson =
          s"""
             |{
             |  "type" : "ItemsList",
             |  "totalResults" : 1,
             |  "results" : [
             |    {
             |      "id" : "$itemId",
             |      "identifiers" : [
             |        {
             |          "identifierType" : {
             |            "id" : "sierra-system-number",
             |            "label" : "Sierra system number",
             |            "type" : "IdentifierType"
             |          },
             |          "value" : "${sierraItemNumber.withCheckDigit}",
             |          "type" : "Identifier"
             |        }
             |      ],
             |      "locations" : [
             |        {
             |          "locationType" : {
             |            "id" : "closed-stores",
             |            "label" : "Closed stores",
             |            "type" : "LocationType"
             |          },
             |          "label" : "locationLabel",
             |          "accessConditions" : [
             |            {
             |              "method" : {
             |                "id" : "online-request",
             |                "label" : "Online request",
             |                "type" : "AccessMethod"
             |              },
             |              "status": {
             |                "id" : "open",
             |                "label" : "Open",
             |                "type" : "AccessStatus"
             |              },
             |              "type" : "AccessCondition"
             |            }
             |          ],
             |          "type" : "PhysicalLocation"
             |        }
             |      ],
             |      "availableDates" : [
             |         {
             |            "from": "2024-04-26T09:00:00.000Z",
             |            "to": "2024-04-26T17:00:00.000Z"
             |         }
             |      ],
             |      "type" : "Item"
             |    }
             |  ]
             |}""".stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.OK

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedJson)
          }
        }
      }
    }

    it("returns an empty list if a work has no items") {
      val resourceName = "work-with-no-items.json"
      val workId = "eccsqg7j"

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(workId),
          catalogueWorkResponse(resourceName)
        )
      )

      withItemsApi(catalogueResponses, Nil, Nil) { _ =>
        val path = s"/works/$workId"

        val expectedJson =
          s"""
                   |{
                   |  "type" : "ItemsList",
                   |  "totalResults" : 0,
                   |  "results" : [
                   |  ]
                   |}
                  """.stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.OK

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedJson)
          }
        }
      }
    }

    it("keeps items as temporarily unavailable if they're at conservation") {
      val resourceName = "work-with-at-conservation-items.json"
      val workId = "n8czw8g7"

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(workId),
          catalogueWorkResponse(resourceName)
        )
      )

      withItemsApi(catalogueResponses, Nil, Nil) { _ =>
        val path = s"/works/$workId"

        val expectedJson =
          s"""
                   |{
                   |  "type" : "ItemsList",
                   |  "totalResults" : 1,
                   |  "results" : [
                   |    {
                   |      "id": "u8br9f3t",
                   |      "identifiers": [
                   |        {
                   |          "identifierType": {
                   |            "id": "sierra-system-number",
                   |            "label": "Sierra system number",
                   |            "type": "IdentifierType"
                   |          },
                   |          "value": "i19520189",
                   |          "type": "Identifier"
                   |        },
                   |        {
                   |          "identifierType": {
                   |            "id": "sierra-identifier",
                   |            "label": "Sierra identifier",
                   |            "type": "IdentifierType"
                   |          },
                   |          "value": "1952018",
                   |          "type": "Identifier"
                   |        }
                   |      ],
                   |      "locations": [
                   |        {
                   |          "locationType": {
                   |            "id": "closed-stores",
                   |            "label": "Closed stores",
                   |            "type": "LocationType"
                   |          },
                   |          "label": "Closed stores",
                   |          "accessConditions": [
                   |            {
                   |              "method": {
                   |                "id": "not-requestable",
                   |                "label": "Not requestable",
                   |                "type": "AccessMethod"
                   |              },
                   |              "status": {
                   |                "id": "temporarily-unavailable",
                   |                "label": "Temporarily unavailable",
                   |                "type": "AccessStatus"
                   |              },
                   |              "note": "This item is undergoing internal assessment or conservation work.",
                   |              "type": "AccessCondition"
                   |            }
                   |          ],
                   |          "type": "PhysicalLocation"
                   |        }
                   |      ],
                   |      "type": "Item"
                   |    }
                   |  ]
                   |}
                  """.stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.OK

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedJson)
          }
        }
      }
    }

    it("handles items which are on loan to a staff member") {
      val resourceName = "work-with-items-on-loan.json"
      val workId = "g2773269"

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(workId),
          catalogueWorkResponse(resourceName)
        )
      )

      val sierraResponses = Seq(
        (
          sierraItemRequest(SierraItemNumber("1835534")),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              f"""
                     |{
                     |  "total": 1,
                     |  "start": 0,
                     |  "entries": [
                     |    ${readResource("sierra-item-on-loan.json")}
                     |  ]
                     |}
                     |""".stripMargin
            )
          )
        )
      )
      withItemsApi(catalogueResponses, sierraResponses, Nil) { _ =>
        val path = s"/works/$workId"

        val expectedJson =
          s"""
                   |{
                   |  "type" : "ItemsList",
                   |  "totalResults" : 1,
                   |  "results" : [
                   |    {
                   |      "id": "ankgmzj2",
                   |      "identifiers": [
                   |        {
                   |          "identifierType": {
                   |            "id": "sierra-system-number",
                   |            "label": "Sierra system number",
                   |            "type": "IdentifierType"
                   |          },
                   |          "value": "i18355341",
                   |          "type": "Identifier"
                   |        },
                   |        {
                   |          "identifierType": {
                   |            "id": "sierra-identifier",
                   |            "label": "Sierra identifier",
                   |            "type": "IdentifierType"
                   |          },
                   |          "value": "1835534",
                   |          "type": "Identifier"
                   |        }
                   |      ],
                   |      "locations": [
                   |        {
                   |          "label": "Medical Collection",
                   |          "accessConditions": [
                   |            {
                   |              "method": {
                   |                "id": "open-shelves",
                   |                "label": "Open shelves",
                   |                "type": "AccessMethod"
                   |              },
                   |              "status": {
                   |                "id": "temporarily-unavailable",
                   |                "label": "Temporarily unavailable",
                   |                "type": "AccessStatus"
                   |              },
                   |              "note": "Item is in use by another reader. Please ask at Library Enquiry Desk.",
                   |              "type": "AccessCondition"
                   |            }
                   |          ],
                   |          "shelfmark": "B105.T54 2004W48h",
                   |          "locationType": {
                   |            "id": "open-shelves",
                   |            "label": "Open shelves",
                   |            "type": "LocationType"
                   |          },
                   |          "type": "PhysicalLocation"
                   |        }
                   |      ],
                   |      "type": "Item"
                   |    }
                   |  ]
                   |}
                  """.stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.OK

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedJson)
          }
        }
      }
    }

    it("returns a 404 if there is no work with this ID") {
      val id = createCanonicalId

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(id),
          catalogueErrorResponse(status = StatusCodes.NotFound)
        )
      )

      withItemsApi(catalogueResponses, Nil, Nil) { _ =>
        val path = s"/works/$id"

        val expectedError =
          s"""
                   |{
                   |  "errorType": "http",
                   |  "httpStatus": 404,
                   |  "label": "Not Found",
                   |  "description": "Work not found for identifier $id",
                   |  "type": "Error"
                   |}""".stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.NotFound

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedError)
          }
        }
      }
    }

    it("returns a 404 if the ID is not a valid canonical ID") {
      val id = "<script>alert('boo!');"

      val catalogueResponses = Seq(
        (
          HttpRequest(
            uri =
              Uri(s"http://catalogue:9001/works/$id?include=identifiers,items")
          ),
          catalogueErrorResponse(status = StatusCodes.NotFound)
        )
      )

      withItemsApi(catalogueResponses, Nil, Nil) { _ =>
        val path = s"/works/$id"

        val expectedError =
          s"""
                   |{
                   |  "errorType": "http",
                   |  "httpStatus": 404,
                   |  "label": "Not Found",
                   |  "description": "Work not found for identifier $id",
                   |  "type": "Error"
                   |}""".stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.NotFound

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedError)
          }
        }
      }
    }

    it("returns a 410 if the original work is a 410") {
      val id = createCanonicalId

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(id),
          catalogueErrorResponse(status = StatusCodes.Gone)
        )
      )

      withItemsApi(catalogueResponses, Nil, Nil) { _ =>
        val path = s"/works/$id"

        val expectedError =
          s"""
                   |{
                   |  "errorType": "http",
                   |  "httpStatus": 410,
                   |  "label": "Gone",
                   |  "description": "This work has been deleted",
                   |  "type": "Error"
                   |}""".stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.Gone

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedError)
          }
        }
      }
    }
  }
}
