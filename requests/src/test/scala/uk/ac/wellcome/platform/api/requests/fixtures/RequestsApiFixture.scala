package uk.ac.wellcome.platform.api.requests.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import weco.http.fixtures.HttpFixtures
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.RequestsApi
import weco.api.stacks.services.ItemLookup

import java.net.URL
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture
    extends ServicesFixture
    with HttpFixtures
    with IndexFixtures {

  val metricsName = "RequestsApiFixture"

  val apiConf =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue",
      contextPath = "context.json"
    )

  def withRequestsApi[R](index: Index)(
    testWith: TestWith[(URL, WireMockServer), R]): R =
    withSierraService {
      case (sierraServiceTest, server) =>
        val indexTest = index

        val router: RequestsApi = new RequestsApi {
          override implicit val ec: ExecutionContext = global
          override implicit val apiConfig: ApiConfig = apiConf
          override val sierraService: SierraService = sierraServiceTest
          override val itemLookup: ItemLookup =
            ItemLookup(elasticClient)(global)
          override val index: Index = indexTest
        }

        withApp(router.routes) { _ =>
          testWith((router.contextUrl, server))
        }
    }
}
