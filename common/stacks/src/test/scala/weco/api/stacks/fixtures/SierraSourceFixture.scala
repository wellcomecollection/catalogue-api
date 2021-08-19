package weco.api.stacks.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import weco.akka.fixtures.Akka
import weco.api.stacks.http.SierraSource
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext.Implicits.global

trait SierraSourceFixture extends HttpFixtures with Akka {
  def withSierraSource[R](
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[SierraSource, R]): R =
    withMaterializer { implicit mat =>
      val httpClient = new MemoryHttpClient(responses) with HttpGet
      with HttpPost {
        override val baseUri: Uri = Uri("http://sierra:1234")
      }

      testWith(new SierraSource(httpClient))
    }
}
