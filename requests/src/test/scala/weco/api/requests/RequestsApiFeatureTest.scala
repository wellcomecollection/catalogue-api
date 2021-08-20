package weco.api.requests

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.utils.JsonAssertions
import weco.catalogue.internal_model.work.generators.{ItemsGenerators, WorkGenerators}
import weco.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.internal_model.index.IndexFixtures
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraPatronNumber
import weco.catalogue.internal_model.Implicits._


class RequestsApiFeatureTest
    extends AnyFunSpec
      with Matchers
      with RequestsApiFixture
      with JsonAssertions
      with IntegrationPatience
      with ItemsGenerators
      with WorkGenerators
      with IndexFixtures
      with SierraIdentifierGenerators {

  describe("requests") {
    it("provides information about a users' holds") {
      val patron = SierraPatronNumber("1234567")
      val itemNumber1 = createSierraItemNumber
      val itemNumber2 = createSierraItemNumber

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri =
              s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0"
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
                 |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                 |      "frozen": false,
                 |      "placed": "2021-05-07",
                 |      "notWantedBeforeDate": "2021-05-07",
                 |      "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
                 |      "status": {"code": "0", "name": "on hold."}
                 |    },
                 |    {
                 |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111",
                 |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${itemNumber2.withoutCheckDigit}",
                 |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                 |      "frozen": false,
                 |      "placed": "2021-05-07",
                 |      "notWantedBeforeDate": "2021-05-07",
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

      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = itemNumber1.withCheckDigit,
          ontologyType = "Item"
        ),
        locations = List.empty,
        title = Some(titleString)
      )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withRequestsApi(elasticClient, index, responses) { _ =>
          val path = s"/users/$patron/item-requests"

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
               |            "value" : "${itemNumber1.withCheckDigit}",
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
}

