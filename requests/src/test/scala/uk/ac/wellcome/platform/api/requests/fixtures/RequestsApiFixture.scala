package uk.ac.wellcome.platform.api.requests.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.RequestsApi
import weco.api.stacks.services.ItemLookup
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture extends HttpFixtures {

  val metricsName = "RequestsApiFixture"

  override def contextUrl =
    new URL("https://localhost/catalogue/context.json")

  implicit val apiConfig: ApiConfig =
    ApiConfig(
      publicHost = "localhost/",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue",
      contextPath = "context.json"
    )

  def withRequestsApi[R](itemLookup: ItemLookup, responses: Seq[(HttpRequest, HttpResponse)] = Seq())(
    testWith: TestWith[URL, R]): R = {
    withMaterializer { implicit mat =>
      val sierraService = SierraService(
        new MemoryHttpClient(responses) with HttpGet with HttpPost {
          override val baseUri: Uri = Uri("http://sierra:1234")
        }
      )

      val api: RequestsApi = new RequestsApi(
        sierraService = sierraService,
        itemLookup = itemLookup
      )

      withApp(api.routes) { _ =>
        testWith(api.contextUrl)
      }
    }
  }
}
