package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.client.WireMock.{
  equalToJson,
  postRequestedFor,
  urlEqualTo
}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.{ItemsGenerators, WorkGenerators}
import uk.ac.wellcome.platform.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraIdentifier
import weco.catalogue.internal_model.identifiers.SourceIdentifier

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class RequestsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with RequestsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with ItemsGenerators
    with WorkGenerators {

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
          identifierType = SierraIdentifier,
          ontologyType = "Item",
          value = "1601017"
        )
      )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withRequestsApi(index) { wireMockServer =>
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
                equalToJson("""
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

    it("returns a 404 Not Found if the ID is not a canonical ID") {
      val itemId = randomAlphanumeric(length = 10)
      val workId = randomAlphanumeric(length = 10)

      withLocalWorksIndex { index =>
        withRequestsApi(index) { _ =>
          val path = "/users/1234567/item-requests"

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "$itemId",
               |  "workId": "$workId",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          val expectedError =
            s"""
               |{
               |  "errorType": "http",
               |  "httpStatus": 404,
               |  "label": "Not Found",
               |  "description": "Item not found for identifier $itemId",
               |  "type": "Error",
               |  "@context": "https://localhostcatalogue/context.json"
               |}""".stripMargin

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.NotFound

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedError)
            }
          }
        }
      }
    }

    it("returns a 404 Not Found if the item doesn't exist") {
      val itemId = createCanonicalId
      val workId = createCanonicalId

      withLocalWorksIndex { index =>
        withRequestsApi(index) { _ =>
          val path = "/users/1234567/item-requests"

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "$itemId",
               |  "workId": "$workId",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          val expectedError =
            s"""
               |{
               |  "errorType": "http",
               |  "httpStatus": 404,
               |  "label": "Not Found",
               |  "description": "Item not found for identifier $itemId",
               |  "type": "Error",
               |  "@context": "https://localhostcatalogue/context.json"
               |}""".stripMargin

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.NotFound

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedError)
            }
          }
        }
      }
    }

    it("responds with a 409 Conflict when a hold is rejected") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraIdentifier,
          ontologyType = "Item",
          value = "1601018"
        )
      )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withRequestsApi(index) { wireMockServer =>
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
                equalToJson("""
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
        val item = createIdentifiedItemWith(
          sourceIdentifier = SourceIdentifier(
            identifierType = SierraIdentifier,
            value = "1292185",
            ontologyType = "Item"
          )
        )

        val work = indexedWork().items(List(item))

        withRequestsApi(index) { _ =>
          insertIntoElasticsearch(index, work)

          val path = "/users/1234567/item-requests"

          // TODO: This output looks distinctly weird.
          // Where's the catalogue ID, for one?
          val expectedJson =
            s"""
               |{
               |  "results" : [
               |    {
               |      "item" : {
               |        "id" : "1292185",
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

            withStringEntity(response.entity) { actualJson =>
              assertJsonStringsAreEqual(actualJson, expectedJson)
            }
          }
        }
      }
    }
  }
  override implicit val ec: ExecutionContext = global
}
