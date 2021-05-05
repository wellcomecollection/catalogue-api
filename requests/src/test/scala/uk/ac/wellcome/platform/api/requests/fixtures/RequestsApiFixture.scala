package uk.ac.wellcome.platform.api.requests.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import java.net.URL

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import weco.http.fixtures.HttpFixtures
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.requests.RequestsApi

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture extends ServicesFixture with HttpFixtures {

  val metricsName = "RequestsApiFixture"

  val contextURLTest = new URL(
    "http://api.wellcomecollection.org/requests/v1/context.json"
  )

  def withRequestsApi[R](testWith: TestWith[WireMockServer, R]): R =
    withStacksService {
      case (stacksService, server) =>
        val router: RequestsApi = new RequestsApi {
          override implicit val ec: ExecutionContext = global
          override implicit val stacksWorkService: StacksService =
            stacksService
        }

        withApp(router.routes) { _ =>
          testWith(server)
        }
    }

}
