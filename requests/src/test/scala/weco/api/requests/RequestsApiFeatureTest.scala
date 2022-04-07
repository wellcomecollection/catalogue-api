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
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  WorkGenerators
}
import weco.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.internal_model.index.IndexFixtures
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraPatronNumber

class RequestsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with RequestsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with ItemsGenerators
    with WorkGenerators
    with IndexFixtures
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
    it("provides information about a users' holds") {
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

      val titleString = randomAlphanumeric(length = 20)

      val item1 = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = itemNumber1.withCheckDigit,
          ontologyType = "Item"
        ),
        locations = List.empty,
        title = Some(titleString)
      )

      val work1 = indexedWork().items(List(item1))

      val item2 = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = itemNumber2.withCheckDigit,
          ontologyType = "Item"
        ),
        locations = List.empty,
        title = Some(titleString)
      )

      val work2 = indexedWork().items(List(item2))

      val catalogueResponses = Seq(
        (
          catalogueItemsRequest(createSierraSystemSourceIdentifierWith(itemNumber1.withCheckDigit), createSierraSystemSourceIdentifierWith(itemNumber2.withCheckDigit)),
          catalogueWorkResponse(Seq(work1, work2))
        ),
      )

      withRequestsApi(sierraResponses = sierraResponses, catalogueResponses = catalogueResponses) { _ =>
        val path = s"/users/$patron/item-requests"

        val expectedJson =
          s"""
             |{
             |  "results" : [
             |    {
             |      "workTitle" : "${work1.data.title.get}",
             |      "workId" : "${work1.state.canonicalId}",
             |      "item" : {
             |        "id" : "${item1.id.canonicalId}",
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
             |        "title" : "$titleString",
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
             |      "workTitle" : "${work2.data.title.get}",
             |      "workId" : "${work2.state.canonicalId}",
             |      "item" : {
             |        "id" : "${item2.id.canonicalId}",
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
             |        "title" : "$titleString",
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
  }
}
