package weco.api.stacks.services

import akka.http.scaladsl.model._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.api.stacks.models._
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.internal_model.locations.{AccessCondition, AccessMethod}
import weco.catalogue.source_model.generators.SierraGenerators
import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}

import scala.concurrent.ExecutionContext.Implicits.global

class SierraServiceTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Akka
    with EitherValues
    with SierraGenerators {

  describe("SierraService") {
    describe("getAccessCondition") {
      it("gets an AccessCondition") {
        val responses = Seq(
          (
            HttpRequest(
              uri =
                "http://sierra:1234/v5/items/1601017?fields=deleted,fixedFields,holdCount,suppressed"
            ),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                  |{
                  |  "id": "1601017",
                  |  "deleted": false,
                  |  "suppressed": false,
                  |  "fixedFields": {
                  |    "79": {"label": "LOCATION", "value": "scmwf", "display": "Closed stores A&MSS Well.Found."},
                  |    "88": {"label": "STATUS", "value": "-", "display": "Available"},
                  |    "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
                  |  },
                  |  "holdCount": 0
                  |}
                  |""".stripMargin
              )
            )
          )
        )

        withMaterializer { implicit mat =>
          val service = SierraService(
            client = new MemoryHttpClient(responses) with HttpGet
            with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val identifier = SourceIdentifier(
            identifierType = SierraSystemNumber,
            value = "i16010176",
            ontologyType = "Item"
          )

          val future = service.getAccessCondition(identifier)

          whenReady(future) {
            _.value shouldBe Some(
              AccessCondition(method = AccessMethod.OnlineRequest)
            )
          }
        }
      }
    }

    describe("getStacksUserHolds") {
      it("gets a StacksUserHolds") {
        val patron = SierraPatronNumber("1234567")

        val item1 = createSierraItemNumber
        val item2 = createSierraItemNumber

        val responses = Seq(
          (
            HttpRequest(
              uri =
                s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0"
            ),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "total": 2,
                   |  "start": 0,
                   |  "entries": [
                   |    {
                   |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111",
                   |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item1.withoutCheckDigit}",
                   |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                   |      "frozen": false,
                   |      "placed": "2021-05-07",
                   |      "notWantedBeforeDate": "2021-05-07",
                   |      "pickupLocation": {"code":"sepbb", "name":"Rare Materials Room"},
                   |      "status": {"code": "0", "name": "on hold."}
                   |    },
                   |    {
                   |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222",
                   |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item2.withoutCheckDigit}",
                   |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                   |      "frozen": false,
                   |      "placed": "2021-05-07",
                   |      "notWantedBeforeDate": "2021-05-07",
                   |      "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
                   |      "status": {"code": "i", "name": "item hold ready for pickup"}
                   |    }
                   |  ]
                   |}
                   |""".stripMargin
              )
            )
          )
        )

        withMaterializer { implicit mat =>
          val service = SierraService(
            new MemoryHttpClient(responses) with HttpGet with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val future = service.getStacksUserHolds(patron)

          whenReady(future) {
            _.value shouldBe StacksUserHolds(
              userId = "1234567",
              holds = List(
                StacksHold(
                  sourceIdentifier = SourceIdentifier(
                    ontologyType = "Item",
                    identifierType = SierraSystemNumber,
                    value = item1.withCheckDigit
                  ),
                  pickup = StacksPickup(
                    location = StacksPickupLocation(
                      id = "sepbb",
                      label = "Rare Materials Room"
                    ),
                    pickUpBy = None
                  ),
                  status = StacksHoldStatus(
                    id = "0",
                    label = "on hold."
                  )
                ),
                StacksHold(
                  sourceIdentifier = SourceIdentifier(
                    ontologyType = "Item",
                    identifierType = SierraSystemNumber,
                    value = item2.withCheckDigit
                  ),
                  pickup = StacksPickup(
                    location = StacksPickupLocation(
                      id = "sotop",
                      label = "Rare Materials Room"
                    ),
                    pickUpBy = None
                  ),
                  status = StacksHoldStatus(
                    id = "i",
                    label = "item hold ready for pickup"
                  )
                )
              )
            )
          }
        }
      }
    }

    describe("placeHold") {
      it("requests a hold from the Sierra API") {
        val patron = SierraPatronNumber("1234567")
        val item = createSierraItemNumber

        val sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          ontologyType = "Item",
          value = item.withCheckDigit
        )

        val responses = Seq(
          (
            HttpRequest(
              method = HttpMethods.POST,
              uri = s"http://sierra:1234/v5/patrons/$patron/holds/requests",
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "recordType": "i",
                   |  "recordNumber": ${item.withoutCheckDigit},
                   |  "pickupLocation": "unspecified"
                   |}
                   |""".stripMargin
              )
            ),
            HttpResponse(status = StatusCodes.NoContent)
          )
        )

        withMaterializer { implicit mat =>
          val service = SierraService(
            new MemoryHttpClient(responses) with HttpGet with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val future = service.placeHold(
            patron = patron,
            sourceIdentifier = sourceIdentifier
          )

          whenReady(future) {
            _.value shouldBe HoldAccepted.HoldCreated
          }
        }
      }

      it("rejects a hold when the Sierra API errors indicating such") {
        val patron = SierraPatronNumber("1234567")
        val item = createSierraItemNumber
        val sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          ontologyType = "Item",
          value = item.withCheckDigit
        )

        val responses = Seq(
          (
            HttpRequest(
              method = HttpMethods.POST,
              uri = s"http://sierra:1234/v5/patrons/$patron/holds/requests",
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "recordType": "i",
                   |  "recordNumber": ${item.withoutCheckDigit},
                   |  "pickupLocation": "unspecified"
                   |}
                   |""".stripMargin
              )
            ),
            HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                  |{
                  |  "code": 132,
                  |  "specificCode": 2,
                  |  "httpStatus": 500,
                  |  "name": "XCirc error",
                  |  "description": "XCirc error : This record is not available"
                  |}
                  |""".stripMargin
              )
            )
          ),
          (
            HttpRequest(
              uri =
                s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0"
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
          ),
          (
            HttpRequest(
              uri =
                s"http://sierra:1234/v5/items/$item?fields=deleted,fixedFields,holdCount,suppressed"
            ),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "id": "$item",
                   |  "deletedDate": "2001-01-01",
                   |  "deleted": false,
                   |  "suppressed": true,
                   |  "fixedFields": {
                   |    "88": {"label": "STATUS", "value": "-", "display": "Available"}
                   |  },
                   |  "holdCount": 0
                   |}
                   |""".stripMargin
              )
            )
          )
        )

        withMaterializer { implicit mat =>
          val service = SierraService(
            new MemoryHttpClient(responses) with HttpGet with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val future = service.placeHold(
            patron = patron,
            sourceIdentifier = sourceIdentifier
          )

          whenReady(future) {
            _.left.value shouldBe HoldRejected.ItemCannotBeRequested
          }
        }
      }
    }
  }
}
