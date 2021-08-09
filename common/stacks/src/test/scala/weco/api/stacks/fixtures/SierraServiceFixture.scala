package weco.api.stacks.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import weco.akka.fixtures.Akka
import weco.api.stacks.services.SierraService
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures
import weco.sierra.models.identifiers.SierraItemNumber

import scala.concurrent.ExecutionContext.Implicits.global

trait SierraServiceFixture extends HttpFixtures with Akka {
  def withSierraService[R](
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[SierraService, R]): R =
    withMaterializer { implicit mat =>
      val httpClient = new MemoryHttpClient(responses) with HttpGet
      with HttpPost {
        override val baseUri: Uri = Uri("http://sierra:1234")
      }

      val sierraService = SierraService(httpClient)

      testWith(sierraService)
    }
}
