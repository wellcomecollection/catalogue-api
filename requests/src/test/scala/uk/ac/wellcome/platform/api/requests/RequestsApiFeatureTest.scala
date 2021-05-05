package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.platform.api.requests.fixtures.RequestsApiFixture

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class RequestsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with RequestsApiFixture
    with JsonAssertions
    with IntegrationPatience {

  describe("requests") {
    it("responds to a GET request") {
      withRequestsApi { _ =>
        val path = "/users/1234567/item-requests"
        whenGetRequestReady(path) {
          _.status shouldBe StatusCodes.OK
        }
      }
    }

    it("accepts requests to place a hold on an item") {
      withRequestsApi { wireMockServer =>
        val path = "/users/1234567/item-requests"

        val entity = createJsonHttpEntityWith(
          """
            |{
            |  "itemId": "ys3ern6x",
            |  "workId": "cnkv77md",
            |  "type": "ItemRequest"
            |}
            |""".stripMargin
        )

        whenPostRequestReady(path, entity) { response =>
          withStringEntity(response.entity) { actualJson =>
            println(actualJson)
          }

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

    it("responds with a 409 Conflict when a hold is rejected") {
      withRequestsApi { wireMockServer =>
        val path = "/users/1234567/item-requests"

        val entity = createJsonHttpEntityWith(
          """
            |{
            |  "itemId": "ys3ern6y",
            |  "workId": "cnkv77md",
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

    it("provides information about a users' holds") {
      withRequestsApi { _ =>
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
             |        "ontologyType" : "Item"
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
  override implicit val ec: ExecutionContext = global
}
