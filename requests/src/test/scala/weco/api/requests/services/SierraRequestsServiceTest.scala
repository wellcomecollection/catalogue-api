package weco.api.requests.services

import java.net.URI

import akka.http.scaladsl.model._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.fixtures.SierraServiceFixture
import weco.api.requests.models.{HoldAccepted, HoldRejected}
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.fields.{SierraHold, SierraHoldStatus, SierraLocation}
import weco.sierra.models.identifiers.SierraPatronNumber

class SierraRequestsServiceTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with EitherValues
    with SierraIdentifierGenerators
    with SierraServiceFixture {

  describe("SierraService") {
    describe("getHolds") {
      it("wraps a response from the Sierra API") {
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

        withSierraService(responses) { service =>
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
                id = new URI(
                  "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111"
                ),
                record = new URI(
                  s"https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item1.withoutCheckDigit}"
                ),
                pickupLocation = SierraLocation("sepbb", "Rare Materials Room"),
                pickupByDate = None,
                status = SierraHoldStatus("0", "on hold.")
              ),
              item2SrcId -> SierraHold(
                id = new URI(
                  "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222"
                ),
                record = new URI(
                  s"https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item2.withoutCheckDigit}"
                ),
                pickupLocation = SierraLocation("sotop", "Rare Materials Room"),
                pickupByDate = None,
                status = SierraHoldStatus("i", "item hold ready for pickup")
              )
            )
          }
        }
      }

      it("fails if Sierra returns an error") {
        val patron = SierraPatronNumber("1234567")

        val responses = Seq(
          (
            HttpRequest(
              uri =
                s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0"
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
                  |  "description": "XCirc error"
                  |}
                  |""".stripMargin
              )
            )
          )
        )

        withSierraService(responses) { service =>
          val future = service.getHolds(patron)

          whenReady(future.failed) { failure =>
            failure.getMessage shouldBe "Sierra error trying to retrieve holds!"
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

        val future = withSierraService(responses) {
          _.placeHold(
            patron = patron,
            sourceIdentifier = sourceIdentifier
          )
        }

        whenReady(future) {
          _.value shouldBe HoldAccepted.HoldCreated
        }
      }

      it("rejects a hold when the Sierra API errors indicating such") {
        val patron = SierraPatronNumber("1234567")
        val itemNumber = createSierraItemNumber
        val sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          ontologyType = "Item",
          value = itemNumber.withCheckDigit
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
                   |  "recordNumber": ${itemNumber.withoutCheckDigit},
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
            createItemRequest(itemNumber),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                f"""
                  |{
                  |  "total": 1,
                  |  "start": 0,
                  |  "entries": [
                  |    {
                  |      "id": "$itemNumber",
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

        val future = withSierraService(responses) {
          _.placeHold(
            patron = patron,
            sourceIdentifier = sourceIdentifier
          )
        }

        whenReady(future) {
          _.left.value shouldBe HoldRejected.ItemCannotBeRequested
        }
      }
    }
  }
}
