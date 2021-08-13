package weco.api.stacks.services

import java.net.URI

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
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.fields.SierraLocation
import weco.sierra.models.identifiers.{SierraItemNumber, SierraPatronNumber}

import scala.concurrent.ExecutionContext.Implicits.global

class SierraServiceTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Akka
    with EitherValues
    with SierraIdentifierGenerators {

  describe("SierraService") {
    describe("getAccessConditions") {
      it("gets AccessConditions") {

        val responses = Seq(
          (
            HttpRequest(
              uri = Uri(
                "http://sierra:1234/v5/items?id=1601017&fields=deleted,fixedFields,holdCount,suppressed"
              )
            ),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                  |{
                  |  "total": 1,
                  |  "start": 0,
                  |  "entries": [
                  |    {
                  |      "id": "1601017",
                  |      "deleted": false,
                  |      "suppressed": false,
                  |      "fixedFields": {
                  |        "79": {"label": "LOCATION", "value": "scmwf", "display": "Closed stores A&MSS Well.Found."},
                  |        "88": {"label": "STATUS", "value": "-", "display": "Available"},
                  |        "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
                  |      },
                  |      "holdCount": 0
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
            client = new MemoryHttpClient(responses) with HttpGet
              with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val itemNumber = SierraItemNumber("1601017")
          val future = service.getAccessConditions(Seq(itemNumber))

          whenReady(future) {
            _ shouldBe Map(
              itemNumber ->
                AccessCondition(method = AccessMethod.OnlineRequest)
            )
          }
        }
      }

      it("if some entries are missing, returns the available AccessConditions") {
        val responses = Seq(
          (
            HttpRequest(
              uri = Uri(
                "http://sierra:1234/v5/items?id=1601017,1234567&fields=deleted,fixedFields,holdCount,suppressed"
              )
            ),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                  |{
                  |  "total": 1,
                  |  "start": 0,
                  |  "entries": [
                  |    {
                  |      "id": "1601017",
                  |      "deleted": false,
                  |      "suppressed": false,
                  |      "fixedFields": {
                  |        "79": {"label": "LOCATION", "value": "scmwf", "display": "Closed stores A&MSS Well.Found."},
                  |        "88": {"label": "STATUS", "value": "-", "display": "Available"},
                  |        "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
                  |      },
                  |      "holdCount": 0
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
            client = new MemoryHttpClient(responses) with HttpGet
              with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val itemNumber = SierraItemNumber("1601017")
          val missingItemNumber = SierraItemNumber("1234567")

          val future =
            service.getAccessConditions(Seq(itemNumber, missingItemNumber))

          whenReady(future) {
            _ shouldBe Map(
              itemNumber ->
                AccessCondition(method = AccessMethod.OnlineRequest)
            )
          }
        }
      }

      it("if all entries are missing, returns no AccessConditions") {
        val responses = Seq(
          (
            HttpRequest(
              uri = Uri(
                "http://sierra:1234/v5/items?id=1601017&fields=deleted,fixedFields,holdCount,suppressed"
              )
            ),
            HttpResponse(
              status = StatusCodes.NotFound,
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                  |{
                  |  "code": 107,
                  |  "specificCode": 0,
                  |  "httpStatus": 404,
                  |  "name": "Record not found"
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

          val itemNumber = SierraItemNumber("1601017")
          val future = service.getAccessConditions(Seq(itemNumber))

          whenReady(future) {
            _ shouldBe Map.empty
          }
        }
      }
    }

    describe("getHolds") {
      it("gets a Map[SourceIdentifier, SierraHold]") {
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

          val future = service.getHolds(patron)

          whenReady(future) { result =>
            val item1SrcId = SourceIdentifier(
              identifierType = SierraSystemNumber,
              ontologyType = "Item",
              value = item1.withCheckDigit
            )

            val item2SrcId = SourceIdentifier(
              identifierType = SierraSystemNumber,
              ontologyType = "Item",
              value = item2.withCheckDigit
            )

            result shouldBe Map(
              item1SrcId -> SierraHold(
                id = new URI("https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111"),
                record = new URI(s"https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item1.withoutCheckDigit}"),
                pickupLocation = SierraLocation("sepbb", "Rare Materials Room"),
                pickupByDate = None,
                status = SierraHoldStatus("0", "on hold.")
              ),
              item2SrcId -> SierraHold(
                id = new URI("https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222"),
                record = new URI(s"https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item2.withoutCheckDigit}"),
                pickupLocation = SierraLocation("sotop", "Rare Materials Room"),
                pickupByDate = None,
                status = SierraHoldStatus("i", "item hold ready for pickup")
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
              uri = Uri(
                f"http://sierra:1234/v5/items?id=$item&fields=deleted,fixedFields,holdCount,suppressed"
              )
            ),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                f"""
                  |{
                  |  "total": 1,
                  |  "start": 0,
                  |  "entries": [
                  |    {
                  |      "id": "$item",
                  |      "deletedDate": "2001-01-01",
                  |      "deleted": false,
                  |      "suppressed": true,
                  |      "fixedFields": {
                  |        "88": {"label": "STATUS", "value": "-", "display": "Available"}
                  |      },
                  |      "holdCount": 0
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
