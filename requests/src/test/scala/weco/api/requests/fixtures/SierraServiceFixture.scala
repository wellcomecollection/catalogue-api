package weco.api.requests.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import weco.akka.fixtures.Akka
import weco.api.requests.services.SierraRequestsService
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures
import weco.sierra.http.SierraSource
import weco.sierra.models.identifiers.SierraItemNumber

import scala.concurrent.ExecutionContext.Implicits.global

trait SierraServiceFixture extends HttpFixtures with Akka {
  def createItemRequest(itemNumber: SierraItemNumber): HttpRequest = {
    val fieldList = SierraSource.requiredItemFields.mkString(",")

    HttpRequest(
      uri =
        s"http://sierra:1234/v5/items?id=$itemNumber&fields=$fieldList"
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
}
