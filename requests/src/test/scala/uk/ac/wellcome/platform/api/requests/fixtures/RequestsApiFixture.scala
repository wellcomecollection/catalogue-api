package uk.ac.wellcome.platform.api.requests.fixtures

import java.net.URL

import com.github.tomakehurst.wiremock.WireMockServer
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import weco.http.fixtures.HttpFixtures
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.RequestsApi

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture extends ServicesFixture with HttpFixtures {

  val metricsName = "RequestsApiFixture"

  override def contextUrl = new URL("https://localhost/catalogue/v2/context.json")
  
  val apiConf =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue",
      contextPath = "context.json"
    )

  def withRequestsApi[R](testWith: TestWith[WireMockServer, R]): R =
    withStacksService {
      case (stacksService, server) =>
        val router: RequestsApi = new RequestsApi {
          override implicit val ec: ExecutionContext = global
          override implicit val stacksWorkService: StacksService =
            stacksService
          override implicit val apiConfig: ApiConfig = apiConf
        }

        withApp(router.routes) { _ =>
          testWith(server)
        }
    }
}
