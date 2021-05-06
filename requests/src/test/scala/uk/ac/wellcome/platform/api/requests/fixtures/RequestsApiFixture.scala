package uk.ac.wellcome.platform.api.requests.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import weco.http.fixtures.HttpFixtures
import uk.ac.wellcome.platform.api.common.services.{SierraService, StacksService}
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.RequestsApi
import weco.api.stacks.services.ItemLookup

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture extends ServicesFixture with HttpFixtures with IndexFixtures {

  val metricsName = "RequestsApiFixture"

  val apiConf =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue",
      contextPath = "context.json"
    )

  def withRequestsApi[R](index: Index)(testWith: TestWith[WireMockServer, R]): R =
    withStacksService {
      case (stacksService, sierraServiceTest, server) =>
        val indexTest = index

        val router: RequestsApi = new RequestsApi {
          override implicit val ec: ExecutionContext = global
          override implicit val stacksWorkService: StacksService =
            stacksService
          override implicit val apiConfig: ApiConfig = apiConf
          override val sierraService: SierraService = sierraServiceTest
          override val itemLookup: ItemLookup = ItemLookup(elasticClient)(global)
          override val index: Index = indexTest
        }

        withApp(router.routes) { _ =>
          testWith(server)
        }
    }
}
