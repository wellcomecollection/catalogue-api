package weco.api.requests

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  StatusCodes
}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.utils.JsonAssertions
import weco.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraPatronNumber

class RequestsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with RequestsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with IdentifiersGenerators
    with SierraIdentifierGenerators
    with ScalatestRouteTest {

  describe("withUserId directive") {
    def userIdRoute =
      RequestsApi.withUserId("a" / Segment / "b")(
        patron => complete(patron.recordNumber)
      )

    it("extracts a user ID from a path") {
      Get("/a/1234567/b") ~> userIdRoute ~> check {
        responseAs[String] shouldBe "1234567"
      }
    }
    it(
      "extracts a user ID from the X-Wellcome-Caller-ID header when the path ID is 'me'"
    ) {
      Get("/a/me/b").withHeaders(RawHeader("X-Wellcome-Caller-ID", "1234567")) ~> userIdRoute ~> check {
        responseAs[String] shouldBe "1234567"
      }
    }
    it(
      "rejects as unauthorized if the X-Wellcome-Caller-ID header is not present when the path ID is 'me'"
    ) {
      Get("/a/me/b") ~> userIdRoute ~> check {
        response.status shouldEqual StatusCodes.Unauthorized
      }
    }
  }

  describe("requests") {
    it("provides information about a user's holds") {
      val patron = SierraPatronNumber("1234567")
      val itemNumber1 = createSierraItemNumber
      val itemNumber2 = createSierraItemNumber

      val workId1 = CanonicalId(randomAlphanumeric(length = 8))
      val workId2 = CanonicalId(randomAlphanumeric(length = 8))

      val itemId1 = CanonicalId(randomAlphanumeric(length = 8))
      val itemId2 = CanonicalId(randomAlphanumeric(length = 8))

      val workTitle1 = randomAlphanumeric()
      val workTitle2 = randomAlphanumeric()

      val itemTitle1 = randomAlphanumeric()
      val itemTitle2 = randomAlphanumeric()

      val sierraResponses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri =
              s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0&fields=id,record,pickupLocation,notNeededAfterDate,note,status"
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "total": 1,
                 |  "start": 0,
                 |  "entries": [
                 |    {
                 |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111",
                 |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${itemNumber1.withoutCheckDigit}",
                 |      "note": "Requested for: 2021-05-07",
                 |      "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
                 |      "status": {"code": "0", "name": "on hold."}
                 |    },
                 |    {
                 |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222",
                 |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${itemNumber2.withoutCheckDigit}",
                 |      "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
                 |      "status": {"code": "0", "name": "on hold."}
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      val catalogueResponses = Seq(
        (
          catalogueItemsRequest(
            itemNumber1.withCheckDigit,
            itemNumber2.withCheckDigit
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "results": [
                 |    {
                 |      "id": "$workId1",
                 |      "title": "$workTitle1",
                 |      "identifiers": [
                 |        {
                 |          "identifierType": {
                 |            "id": "calm-record-id",
                 |            "label": "Calm RecordIdentifier",
                 |            "type": "IdentifierType"
                 |          },
                 |          "value": "21FxkWhG4b",
                 |          "type": "Identifier"
                 |        }
                 |      ],
                 |      "items": [
                 |        {
                 |          "id": "$itemId1",
                 |          "identifiers": [
                 |            {
                 |              "identifierType": {
                 |                "id": "sierra-system-number",
                 |                "label": "Sierra system number",
                 |                "type": "IdentifierType"
                 |              },
                 |              "value": "${itemNumber1.withCheckDigit}",
                 |              "type": "Identifier"
                 |            }
                 |          ],
                 |          "title": "$itemTitle1",
                 |          "locations": [],
                 |          "type": "Item"
                 |        }
                 |      ]
                 |    },
                 |    {
                 |      "id": "$workId2",
                 |      "title": "$workTitle2",
                 |      "identifiers": [
                 |        {
                 |          "identifierType": {
                 |            "id": "miro-image-number",
                 |            "label": "Miro image number",
                 |            "type": "IdentifierType"
                 |          },
                 |          "value": "nysdKFCmtI",
                 |          "type": "Identifier"
                 |        }
                 |      ],
                 |      "items": [
                 |        {
                 |          "id": "$itemId2",
                 |          "identifiers": [
                 |            {
                 |              "identifierType": {
                 |                "id": "sierra-system-number",
                 |                "label": "Sierra system number",
                 |                "type": "IdentifierType"
                 |              },
                 |              "value": "${itemNumber2.withCheckDigit}",
                 |              "type": "Identifier"
                 |            }
                 |          ],
                 |          "title": "$itemTitle2",
                 |          "locations": [],
                 |          "type": "Item"
                 |        }
                 |      ]
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withRequestsApi(
        sierraResponses = sierraResponses,
        catalogueResponses = catalogueResponses
      ) { _ =>
        val path = s"/users/$patron/item-requests"

        val expectedJson =
          s"""
             |{
             |  "results" : [
             |    {
             |      "workTitle" : "$workTitle1",
             |      "workId" : "$workId1",
             |      "item" : {
             |        "id" : "$itemId1",
             |        "identifiers" : [
             |          {
             |            "identifierType" : {
             |              "id" : "sierra-system-number",
             |              "label" : "Sierra system number",
             |              "type" : "IdentifierType"
             |            },
             |            "value" : "${itemNumber1.withCheckDigit}",
             |            "type" : "Identifier"
             |          }
             |        ],
             |        "title" : "$itemTitle1",
             |        "locations" : [
             |        ],
             |        "type" : "Item"
             |      },
             |      "pickupDate" : "2021-05-07",
             |      "pickupLocation" : {
             |        "id" : "sotop",
             |        "label" : "Rare Materials Room",
             |        "type" : "LocationDescription"
             |      },
             |      "status" : {
             |        "id" : "0",
             |        "label" : "on hold.",
             |        "type" : "RequestStatus"
             |      },
             |      "type" : "Request"
             |    },
             |    {
             |      "workTitle" : "$workTitle2",
             |      "workId" : "$workId2",
             |      "item" : {
             |        "id" : "$itemId2",
             |        "identifiers" : [
             |          {
             |            "identifierType" : {
             |              "id" : "sierra-system-number",
             |              "label" : "Sierra system number",
             |              "type" : "IdentifierType"
             |            },
             |            "value" : "${itemNumber2.withCheckDigit}",
             |            "type" : "Identifier"
             |          }
             |        ],
             |        "title" : "$itemTitle2",
             |        "locations" : [
             |        ],
             |        "type" : "Item"
             |      },
             |      "pickupLocation" : {
             |        "id" : "sotop",
             |        "label" : "Rare Materials Room",
             |        "type" : "LocationDescription"
             |      },
             |      "status" : {
             |        "id" : "0",
             |        "label" : "on hold.",
             |        "type" : "RequestStatus"
             |      },
             |      "type" : "Request"
             |    }
             |  ],
             |  "totalResults" : 2,
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

    it("returns an empty list if a user has no holds") {
      val patron = SierraPatronNumber("1234567")

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri =
              s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0&fields=id,record,pickupLocation,notNeededAfterDate,note,status"
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "total": 0,
                 |  "start": 0,
                 |  "entries": []
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withRequestsApi(responses) { _ =>
        val path = s"/users/$patron/item-requests"

        val expectedJson =
          s"""
             |{
             |  "results" : [],
             |  "totalResults" : 0,
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

    it("surfaces unexpected errors when finding a user's holds") {
      val patron = SierraPatronNumber("1234567")
      val itemNumber1 = createSierraItemNumber
      val itemNumber2 = createSierraItemNumber

      val sierraResponses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri =
              s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0&fields=id,record,pickupLocation,notNeededAfterDate,note,status"
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "total": 1,
                 |  "start": 0,
                 |  "entries": [
                 |    {
                 |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111",
                 |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${itemNumber1.withoutCheckDigit}",
                 |      "note": "Requested for: 2021-05-07",
                 |      "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
                 |      "status": {"code": "0", "name": "on hold."}
                 |    },
                 |    {
                 |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222",
                 |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${itemNumber2.withoutCheckDigit}",
                 |      "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
                 |      "status": {"code": "0", "name": "on hold."}
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      val catalogueResponses = Seq(
        (
          catalogueItemsRequest(itemNumber1.withCheckDigit, itemNumber2.withCheckDigit),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """{ "bleep": "bloop" }"""
            )
          )
        )
      )

      withRequestsApi(
        sierraResponses = sierraResponses,
        catalogueResponses = catalogueResponses
      ) { _ =>
        val path = s"/users/$patron/item-requests"

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.InternalServerError

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(
              _,
              """
               {
                 "errorType" : "http",
                 "httpStatus" : 500,
                 "label" : "Internal Server Error",
                 "type" : "Error"
               }
              """.stripMargin
            )
          }
        }
      }
    }
  }
}
