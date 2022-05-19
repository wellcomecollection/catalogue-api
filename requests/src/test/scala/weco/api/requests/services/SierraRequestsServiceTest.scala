package weco.api.requests.services

import java.net.URI
import akka.http.scaladsl.model._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.fixtures.SierraServiceFixture
import weco.api.requests.models.{HoldAccepted, HoldRejected}
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.locations.{
  DisplayAccessCondition,
  DisplayAccessMethod,
  DisplayAccessStatus
}
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.fields.{SierraHold, SierraHoldStatus, SierraLocation}
import weco.sierra.models.identifiers.SierraPatronNumber

import java.time.LocalDate

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
            createListHoldsRequest(patron),
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
                   |      "note": "Requested for: 18-02-2022",
                   |      "pickupLocation": {"code":"sepbb", "name":"Rare Materials Room"},
                   |      "status": {"code": "0", "name": "on hold."}
                   |    },
                   |    {
                   |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222",
                   |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item2.withoutCheckDigit}",
                   |      "note": "Requested for: 19-02-2022",
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
                notNeededAfterDate = None,
                note = Some("Requested for: 18-02-2022"),
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
                notNeededAfterDate = None,
                note = Some("Requested for: 19-02-2022"),
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
            createListHoldsRequest(patron),
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

      it("ignores holds that correspond to inter-library-records") {
        val patron = createSierraPatronNumber

        val item = createSierraItemNumber

        val responses = Seq(
          (
            createListHoldsRequest(patron),
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
                   |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/101339@illd",
                   |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                   |      "frozen": false,
                   |      "placed": "2021-01-12",
                   |      "notWantedBeforeDate": "2021-01-12",
                   |      "pickupLocation": {"code": "illd", "name": "ILL Department"},
                   |      "status": {"code": "0", "name": "on hold."}
                   |    },
                   |    {
                   |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222",
                   |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item.withoutCheckDigit}",
                   |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                   |      "frozen": false,
                   |      "placed": "2021-05-07",
                   |      "notWantedBeforeDate": "2021-05-07",
                   |      "pickupLocation": {"code": "sotop", "name": "Rare Materials Room"},
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
            val itemSrcId = SourceIdentifier(
              identifierType = SierraSystemNumber,
              ontologyType = "Item",
              value = item.withCheckDigit
            )

            result shouldBe Map(
              itemSrcId -> SierraHold(
                id = new URI(
                  "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222"
                ),
                record = new URI(
                  s"https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item.withoutCheckDigit}"
                ),
                pickupLocation = SierraLocation("sotop", "Rare Materials Room"),
                notNeededAfterDate = None,
                note = None,
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
        val pickupDateString = "2022-02-18"
        val pickupDate = LocalDate.parse(pickupDateString)

        val sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          ontologyType = "Item",
          value = item.withCheckDigit
        )

        val responses = Seq(
          (
            createHoldRequest(patron, item, pickupDate),
            HttpResponse(status = StatusCodes.NoContent)
          )
        )

        val future = withSierraService(responses) {
          _.placeHold(
            patron = patron,
            sourceIdentifier = DisplayIdentifier(sourceIdentifier),
            pickupDate = Some(pickupDate)
          )
        }

        whenReady(future) {
          _.value shouldBe HoldAccepted.HoldCreated
        }
      }

      it("rejects a hold when the Sierra API errors indicating such") {
        val patron = SierraPatronNumber("1234567")
        val itemNumber = createSierraItemNumber
        val pickupDateString = "2022-02-18"
        val pickupDate = LocalDate.parse(pickupDateString)
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
                   |  "note": "Requested for: 18-02-2022",
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
            sourceIdentifier = DisplayIdentifier(sourceIdentifier),
            pickupDate = Some(pickupDate)
          )
        }

        whenReady(future) {
          _.left.value shouldBe HoldRejected.ItemCannotBeRequested
        }
      }

      it(
        "rejects a hold if the Catalogue API item is neither stale nor requestable"
      ) {
        val patron = SierraPatronNumber("1234567")
        val item = createSierraItemNumber
        val pickupDateString = "2022-02-18"
        val pickupDate = LocalDate.parse(pickupDateString)

        val sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          ontologyType = "Item",
          value = item.withCheckDigit
        )

        val responses = Seq(
          (
            createHoldRequest(patron, item, pickupDate),
            HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
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
            createListHoldsRequest(patron),
            createListHoldsResponse(patron, items = List())
          )
          // If the requests service thought the access condition was stale or
          // that the item might be requestable, we'd expect to see it make
          // a third API call to Sierra, to get fresh item data.
          //
          // If it tries to make that request during this test, the test will fail
          // because the in-memory Sierra API client doesn't have a request/response
          // defined in this list.
        )

        val future = withSierraService(responses) {
          _.placeHold(
            patron = patron,
            sourceIdentifier = DisplayIdentifier(sourceIdentifier),
            pickupDate = Some(pickupDate),
            accessCondition = Some(
              DisplayAccessCondition(
                method = DisplayAccessMethod(
                  id = "not-requestable",
                  label = "Not requestable"
                ),
                status = Some(
                  DisplayAccessStatus(
                    id = "unavailable",
                    label = "Unavailable"
                  )
                ),
                terms = None,
                note = None
              )
            )
          )
        }

        whenReady(future) {
          _.left.value shouldBe HoldRejected.ItemCannotBeRequested
        }
      }
    }
  }
}
