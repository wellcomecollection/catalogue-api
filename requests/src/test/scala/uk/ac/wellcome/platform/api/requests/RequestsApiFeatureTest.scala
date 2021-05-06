package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.{ItemsGenerators, WorkGenerators}
import uk.ac.wellcome.platform.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.identifiers.IdentifierType.{MiroImageNumber, SierraSystemNumber}
import weco.catalogue.internal_model.identifiers.{CanonicalId, SourceIdentifier}

import scala.util.{Failure, Try}

class RequestsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with RequestsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with WorkGenerators
    with ItemsGenerators {

  describe("requests") {
    it("responds to a GET request") {
      withLocalWorksIndex { index =>
        withRequestsApi(index) { _ =>
          val path = "/users/1234567/item-requests"
          whenGetRequestReady(path) {
            _.status shouldBe StatusCodes.OK
          }
        }
      }
    }

    it("accepts requests to place a hold on an item") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = "i16010176",
          ontologyType = "Item"
        )
      )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withRequestsApi(index) { case (_, wireMockServer) =>
          val path = "/users/1234567/item-requests"

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "${item.id.canonicalId}",
               |  "workId": "${work.state.canonicalId}",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.Accepted

            wireMockServer.verify(
              1,
              postRequestedFor(
                urlEqualTo(
                  "/iii/sierra-api/v5/patrons/1234567/holds/requests"
                )
              ).withRequestBody(
                equalToJson(
                  """
                    |{
                    |  "recordType" : "i",
                    |  "recordNumber" : 1601017,
                    |  "pickupLocation" : "unspecified"
                    |}
                    |""".stripMargin)
              )
            )

            response.entity.isKnownEmpty() shouldBe true
          }
        }
      }
    }

    it("responds with a 409 Conflict when a hold is rejected") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = "i16010188",
          ontologyType = "Item"
        )
      )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withRequestsApi(index) { case (_, wireMockServer) =>
          val path = "/users/1234567/item-requests"

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "${item.id.canonicalId}",
               |  "workId": "${work.state.canonicalId}",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.Conflict

            wireMockServer.verify(
              1,
              postRequestedFor(
                urlEqualTo(
                  "/iii/sierra-api/v5/patrons/1234567/holds/requests"
                )
              ).withRequestBody(
                equalToJson(
                  """
                    |{
                    |  "recordType" : "i",
                    |  "recordNumber" : 1601018,
                    |  "pickupLocation" : "unspecified"
                    |}
                    |""".stripMargin)
              )
            )
          }
        }
      }
    }

    it("provides information about a users' holds") {
      withLocalWorksIndex { index =>
        withRequestsApi(index) { _ =>
          val path = "/users/1234567/item-requests"

          val expectedJson =
            s"""
               |{
               |  "results" : [
               |    {
               |      "item" : {
               |        "id" : "n5v7b4md",
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
  }

  describe("placing a hold") {
    it("returns a 404 if the item ID isn't a canonical ID") {
      withLocalWorksIndex { index =>
        withRequestsApi(index) { case (contextUrl, _) =>
          val path = "/users/1234567/item-requests"

          val itemId = randomAlphanumeric(length = 10)
          Try { CanonicalId(itemId) } shouldBe a[Failure[_]]

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
    }

    it("returns a 404 if there is no item with this ID") {
      withLocalWorksIndex { index =>
        withRequestsApi(index) { case (contextUrl, _) =>
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
    }

    it("returns a 400 if the item isn't a Sierra item") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = MiroImageNumber,
          value = "V0012345",
          ontologyType = "Item"
        )
      )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withRequestsApi(index) { case (contextUrl, _) =>
          val path = "/users/1234567/item-requests"

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "${item.id.canonicalId}",
               |  "workId": "${work.state.canonicalId}",
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
}
