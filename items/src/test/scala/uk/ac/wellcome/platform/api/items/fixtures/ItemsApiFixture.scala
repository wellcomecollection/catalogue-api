package uk.ac.wellcome.platform.api.items.fixtures

import java.net.URL

import com.github.tomakehurst.wiremock.WireMockServer
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.items.ItemsApi
import uk.ac.wellcome.platform.api.models.ApiConfig
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiFixture extends ServicesFixture with HttpFixtures {

  val metricsName = "ItemsApiFixture"

  val contextURLTest = new URL(
    "http://api.wellcomecollection.org/stacks/v1/context.json"
  )

  val apiConf =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue",
      contextPath = "context.json"
    )

  def withItemsApi[R](testWith: TestWith[WireMockServer, R]): R =

    withStacksService {
      case (stacksService, server) =>
        val router: ItemsApi = new ItemsApi {
          override implicit val ec: ExecutionContext = global
          override implicit val stacksWorkService: StacksService =
            stacksService
          override implicit val apiConfig: ApiConfig = apiConf

          override def context: String = contextUri
        }

        withApp(router.routes) { _ =>
          testWith(server)
        }

    }
}
