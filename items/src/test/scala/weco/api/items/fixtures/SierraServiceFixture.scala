package weco.api.items.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.Materializer
import org.scalatest.Suite
import weco.api.stacks.services.SierraService
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.source_model.generators.SierraGenerators
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext.Implicits.global


trait SierraServiceFixture
  extends HttpFixtures
    with IndexFixtures
    with SierraGenerators {

  this: Suite =>

  def withSierraService[R](
                            responses: Seq[(HttpRequest, HttpResponse)] = Seq()
                          )(testWith: TestWith[SierraService, R])(implicit mat: Materializer): R = {
    val httpClient = new MemoryHttpClient(responses) with HttpGet
      with HttpPost {
      override val baseUri: Uri = Uri("http://sierra:1234")
    }

    val sierraService = SierraService(httpClient)
    testWith(sierraService)

  }
  def sierraUri(sierraItemNumber: SierraItemNumber) =
    Uri(f"http://sierra:1234/v5/items/${sierraItemNumber.withoutCheckDigit}?fields=deleted,fixedFields,holdCount,suppressed")

}
