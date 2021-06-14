package uk.ac.wellcome.platform.api.requests.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.RequestsApi
import weco.api.stacks.services.ItemLookup
import weco.http.fixtures.HttpFixtures

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture extends ServicesFixture with HttpFixtures {

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

  def withRequestsApi[R](itemLookup: ItemLookup)(
    testWith: TestWith[(URL, WireMockServer), R]): R =
    withSierraService {
      case (sierraService, server) =>
        val api: RequestsApi = new RequestsApi(
          sierraService = sierraService,
          itemLookup = itemLookup
        )

        withApp(api.routes) { _ =>
          testWith((api.contextUrl, server))
        }
    }
}
