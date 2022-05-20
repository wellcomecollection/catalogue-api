package weco.api.requests.fixtures

import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  Uri
}
import weco.akka.fixtures.Akka
import weco.api.requests.services.SierraRequestsService
import weco.fixtures.{RandomGenerators, TestWith}
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures
import weco.sierra.http.SierraSource
import weco.sierra.models.identifiers.{SierraItemNumber, SierraPatronNumber}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global

trait SierraServiceFixture
    extends HttpFixtures
    with Akka
    with RandomGenerators {
  def createItemRequest(itemNumber: SierraItemNumber): HttpRequest = {
    val fieldList = SierraSource.requiredItemFields.mkString(",")

    HttpRequest(
      uri = s"http://sierra:1234/v5/items?id=$itemNumber&fields=$fieldList"
    )
  }

  def withSierraService[R](
    responses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    holdLimit: Int = 10
  )(testWith: TestWith[SierraRequestsService, R]): R =
    withMaterializer { implicit mat =>
      val httpClient = new MemoryHttpClient(responses) with HttpGet
      with HttpPost {
        override val baseUri: Uri = Uri("http://sierra:1234")
      }

      val sierraService =
        SierraRequestsService(httpClient, holdLimit = holdLimit)

      testWith(sierraService)
    }

  def createHoldRequest(
    patron: SierraPatronNumber,
    item: SierraItemNumber,
    pickupDate: LocalDate
  ): HttpRequest =
    HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://sierra:1234/v5/patrons/$patron/holds/requests",
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        s"""
           |{
           |  "recordType": "i",
           |  "recordNumber": ${item.withoutCheckDigit},
           |  "note": "Requested for: ${DateTimeFormatter
             .ofPattern("yyyy-MM-dd")
             .format(pickupDate)}",
           |  "pickupLocation": "unspecified"
           |}
           |""".stripMargin
      )
    )

  def createListHoldsRequest(patron: SierraPatronNumber): HttpRequest =
    HttpRequest(
      method = HttpMethods.GET,
      uri =
        s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0&fields=id,record,pickupLocation,notNeededAfterDate,note,status"
    )

  private def createListHoldEntry(
    patron: SierraPatronNumber,
    item: SierraItemNumber
  ): String = {
    val holdId = randomInt(from = 0, to = 10000)

    s"""
       |{
       |  "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/$holdId",
       |  "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item.withoutCheckDigit}",
       |  "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
       |  "frozen": false,
       |  "placed": "2021-05-07",
       |  "notWantedBeforeDate": "2021-05-07",
       |  "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
       |  "status": {"code": "0", "name": "on hold."}
       |}
       |""".stripMargin
  }

  def createListHoldsResponse(
    patron: SierraPatronNumber,
    items: Seq[SierraItemNumber]
  ): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        s"""
           |{
           |  "total": ${items.size},
           |  "start": 0,
           |  "entries": [
           |    ${items
             .map(it => createListHoldEntry(patron, it))
             .mkString(",")}
           |  ]
           |}
           |""".stripMargin
      )
    )
}
