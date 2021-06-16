package weco.api.stacks.http

import akka.http.scaladsl.model._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import weco.api.stacks.models.{
  SierraErrorCode,
  SierraHold,
  SierraHoldStatus,
  SierraHoldsList,
  SierraItem,
  SierraItemStatus
}
import weco.catalogue.source_model.sierra.identifiers.{
  SierraItemNumber,
  SierraPatronNumber
}
import weco.catalogue.source_model.sierra.source.SierraSourceLocation
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global

class SierraSourceTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with Akka
    with ScalaFutures
    with IntegrationPatience {
  def withSource[R](responses: Seq[(HttpRequest, HttpResponse)])(
    testWith: TestWith[SierraSource, R]): R =
    withMaterializer { implicit mat =>
      val client =
        new MemoryHttpClient(responses = responses) with HttpGet with HttpPost {
          override val baseUri: Uri = Uri("http://sierra:1234")
        }

      val source = new SierraSource(client)

      testWith(source)
    }

  describe("lookupItem") {
    it("looks up a single item") {
      val itemNumber = SierraItemNumber("1146055")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri(
              "http://sierra:1234/v5/items/1146055?fields=deleted,holdCount,status,suppressed")
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "id": "1146055",
                |  "updatedDate": "2021-06-09T13:23:27Z",
                |  "createdDate": "1999-11-15T18:56:00Z",
                |  "deleted": false,
                |  "bibIds": [
                |    "1126829"
                |  ],
                |  "location": {
                |    "code": "sgmed",
                |    "name": "Closed stores Med."
                |  },
                |  "status": {
                |    "code": "t",
                |    "display": "In quarantine"
                |  },
                |  "volumes": [],
                |  "barcode": "22500271327",
                |  "callNumber": "K33043"
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItem(itemNumber)

        whenReady(future) {
          _ shouldBe Right(
            SierraItem(
              id = itemNumber,
              deleted = false,
              status = Some(
                SierraItemStatus(
                  code = "t",
                  display = "In quarantine"
                ))
            )
          )
        }
      }
    }

    it("looks up a non-existent item") {
      val itemNumber = SierraItemNumber("1000000")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri(
              "http://sierra:1234/v5/items/1000000?fields=deleted,holdCount,status,suppressed")
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

      withSource(responses) { source =>
        val future = source.lookupItem(itemNumber)

        whenReady(future) {
          _ shouldBe Left(SierraItemLookupError.ItemNotFound)
        }
      }
    }

    it("looks up a deleted item") {
      val itemNumber = SierraItemNumber("1000001")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri(
              "http://sierra:1234/v5/items/1000001?fields=deleted,holdCount,status,suppressed")
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "id": "1000001",
                |  "deletedDate": "2004-04-14",
                |  "deleted": true,
                |  "bibIds": [],
                |  "volumes": []
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItem(itemNumber)

        whenReady(future) {
          _.value shouldBe SierraItem(
            id = SierraItemNumber("1000001"),
            deleted = true,
            status = None
          )
        }
      }
    }
  }

  describe("listHolds") {
    it("looks up the holds for a user") {
      val patron = SierraPatronNumber("1234567")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds?limit=100&offset=0")
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
                |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/1111111",
                |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                |      "frozen": false,
                |      "placed": "2021-05-07",
                |      "notWantedBeforeDate": "2021-05-07",
                |      "pickupLocation": {
                |        "code": "sotop",
                |        "name": "Rare Materials Room"
                |      },
                |      "status": {
                |        "code": "0",
                |        "name": "on hold."
                |      },
                |      "recordType": "i",
                |      "priority": 1
                |    },
                |    {
                |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222",
                |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/2222222",
                |      "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
                |      "frozen": false,
                |      "placed": "2021-05-11",
                |      "notWantedBeforeDate": "2021-05-11",
                |      "pickupLocation": {
                |        "code": "hgser",
                |        "name": "Library Enquiry Desk"
                |      },
                |      "status": {
                |        "code": "i",
                |        "name": "item hold ready for pickup."
                |      },
                |      "recordType": "i",
                |      "priority": 1
                |    }
                |  ]
                |}
                |
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.listHolds(patron)

        whenReady(future) {
          _.value shouldBe
            SierraHoldsList(
              total = 2,
              entries = List(
                SierraHold(
                  id = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111"),
                  record = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/1111111"),
                  pickupLocation = SierraSourceLocation(
                    code = "sotop",
                    name = "Rare Materials Room"),
                  pickupByDate = None,
                  status = SierraHoldStatus(code = "0", name = "on hold.")
                ),
                SierraHold(
                  id = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222"),
                  record = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/2222222"),
                  pickupLocation = SierraSourceLocation(
                    code = "hgser",
                    name = "Library Enquiry Desk"),
                  pickupByDate = None,
                  status = SierraHoldStatus(
                    code = "i",
                    name = "item hold ready for pickup.")
                )
              )
            )
        }
      }
    }

    it("looks up an empty list of holds") {
      val patron = SierraPatronNumber("1234567")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds?limit=100&offset=0")
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
                 |
                 |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.listHolds(patron)

        whenReady(future) {
          _.value shouldBe SierraHoldsList(total = 0, entries = List())
        }
      }
    }
  }

  describe("createHold") {
    it("creates a hold") {
      val patron = SierraPatronNumber("1234567")
      val item = SierraItemNumber("1111111")

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds/requests"),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""{"recordType":"i","recordNumber":${item.withoutCheckDigit},"pickupLocation":"unspecified"}"""
            )
          ),
          HttpResponse(
            status = StatusCodes.NoContent,
            entity = HttpEntity.Empty
          )
        )
      )

      withSource(responses) { source =>
        val future = source.createHold(patron, item)

        whenReady(future) {
          _.value shouldBe (())
        }
      }
    }

    it("returns an error if the hold can't be placed") {
      val patron = SierraPatronNumber("1234567")
      val item = SierraItemNumber("1111111")

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds/requests"),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""{"recordType":"i","recordNumber":${item.withoutCheckDigit},"pickupLocation":"unspecified"}"""
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
        )
      )

      withSource(responses) { source =>
        val future = source.createHold(patron, item)

        whenReady(future) {
          _.left.value shouldBe SierraErrorCode(
            code = 132,
            specificCode = 2,
            httpStatus = 500,
            name = "XCirc error",
            description = Some("XCirc error : This record is not available")
          )
        }
      }
    }
  }
}
