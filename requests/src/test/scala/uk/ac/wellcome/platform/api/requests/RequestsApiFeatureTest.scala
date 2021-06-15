package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.generators.ItemsGenerators
import uk.ac.wellcome.platform.api.requests.fixtures.RequestsApiFixture
import weco.api.stacks.services.memory.MemoryItemLookup
import weco.catalogue.internal_model.identifiers.IdentifierType.{
  MiroImageNumber,
  SierraSystemNumber
}
import weco.catalogue.internal_model.identifiers.{CanonicalId, SourceIdentifier}

import scala.util.{Failure, Try}

class RequestsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with RequestsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with ItemsGenerators {

  describe("requests") {
    it("provides information about a users' holds") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = "i12921853",
          ontologyType = "Item"
        )
      )

      val lookup = new MemoryItemLookup(items = Seq(item))

      withRequestsApi(lookup) { _ =>
        val path = "/users/1234567/item-requests"

        val expectedJson =
          s"""
             |{
             |  "results" : [
             |    {
             |      "item" : {
             |        "id" : "${item.id.canonicalId}",
             |        "identifiers" : [
             |          {
             |            "identifierType" : {
             |              "id" : "sierra-system-number",
             |              "label" : "Sierra system number",
             |              "type" : "IdentifierType"
             |            },
             |            "value" : "i12921853",
             |            "type" : "Identifier"
             |          }
             |        ],
             |        "locations" : [
             |        ],
             |        "type" : "Item"
             |      },
             |      "pickupDate" : "2019-12-03T04:00:00Z",
             |      "pickupLocation" : {
             |        "id" : "sepbb",
             |        "label" : "Rare Materials Room",
             |        "type" : "LocationDescription"
             |      },
             |      "status" : {
             |        "id" : "i",
             |        "label" : "item hold ready for pickup.",
             |        "type" : "RequestStatus"
             |      },
             |      "type" : "Request"
             |    }
             |  ],
             |  "totalResults" : 1,
             |  "type" : "ResultList"
             |}""".stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.OK

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedJson)
          }
        }
      }
    }
  }

  describe("placing a hold") {
    it("returns a 404 if the item ID isn't a canonical ID") {
      val lookup = new MemoryItemLookup(items = Seq.empty)

      withRequestsApi(lookup) {
        case (contextUrl, _) =>
          val path = "/users/1234567/item-requests"

          val itemId = randomAlphanumeric(length = 10)
          Try {
            CanonicalId(itemId)
          } shouldBe a[Failure[_]]

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "$itemId",
               |  "workId": "$createCanonicalId",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.NotFound

            val expectedError =
              s"""
                 |{
                 |  "errorType": "http",
                 |  "httpStatus": 404,
                 |  "label": "Not Found",
                 |  "description": "Item not found for identifier $itemId",
                 |  "type": "Error",
                 |  "@context": "$contextUrl"
                 |}""".stripMargin

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedError)
            }
          }
      }
    }

    it("returns a 404 if there is no item with this ID") {
      val lookup = new MemoryItemLookup(items = Seq.empty)

      withRequestsApi(lookup) {
        case (contextUrl, _) =>
          val path = "/users/1234567/item-requests"

          val itemId = createCanonicalId

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "$itemId",
               |  "workId": "$createCanonicalId",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.NotFound

            val expectedError =
              s"""
                 |{
                 |  "errorType": "http",
                 |  "httpStatus": 404,
                 |  "label": "Not Found",
                 |  "description": "Item not found for identifier $itemId",
                 |  "type": "Error",
                 |  "@context": "$contextUrl"
                 |}""".stripMargin

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedError)
            }
          }
      }
    }

    it("returns a 400 if the item isn't a Sierra item") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = MiroImageNumber,
          value = "V0012345",
          ontologyType = "Item"
        )
      )

      val lookup = new MemoryItemLookup(items = Seq(item))

      withRequestsApi(lookup) {
        case (contextUrl, _) =>
          val path = "/users/1234567/item-requests"

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "${item.id.canonicalId}",
               |  "workId": "$createCanonicalId",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.BadRequest

            val expectedError =
              s"""
                 |{
                 |  "errorType": "http",
                 |  "httpStatus": 400,
                 |  "label": "Bad Request",
                 |  "description": "You cannot request ${item.id.canonicalId}",
                 |  "type": "Error",
                 |  "@context": "$contextUrl"
                 |}""".stripMargin

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedError)
            }
          }
      }
    }
  }
}
