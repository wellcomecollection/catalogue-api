package weco.api.items

import akka.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.items.fixtures.{ItemsApiFixture, ItemsApiGenerators}
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.json.utils.JsonAssertions
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraItemNumber

import scala.util.{Failure, Try}

class ItemsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with ItemsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with ItemsApiGenerators
    with SierraIdentifierGenerators {

  describe("look up the status of an item") {
    it("shows a user the items on a work") {
      val resourceName = "work-with-temporarily-unavailable-item.json"
      val workId = CanonicalId("eccsqg7j")
      val itemId = CanonicalId("otdlfo0u")
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

      withItemsApi(catalogueResponses, sierraResponses) { _ =>
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
      val workId = CanonicalId("eccsqg7j")

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(workId),
          catalogueWorkResponse(resourceName)
        )
      )

      withItemsApi(catalogueResponses) { _ =>
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

    it("returns a 404 if there is no work with this ID") {
      val id = createCanonicalId

      val catalogueResponses = Seq(
        (
          catalogueWorkRequest(id),
          catalogueErrorResponse(status = StatusCodes.NotFound)
        )
      )

      withItemsApi(catalogueResponses) { _ =>
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
      val id = randomAlphanumeric(length = 10)
      Try {
        CanonicalId(id)
      } shouldBe a[Failure[_]]

      val catalogueResponses = Seq(
        (
          HttpRequest(
            uri =
              Uri(s"http://catalogue:9001/works/$id?include=identifiers,items")
          ),
          catalogueErrorResponse(status = StatusCodes.NotFound)
        )
      )

      withItemsApi(catalogueResponses) { _ =>
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

      withItemsApi(catalogueResponses) { _ =>
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
