package uk.ac.wellcome.platform.api.items.fixtures

import java.net.URL
import com.github.tomakehurst.wiremock.WireMockServer
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.items.ItemsApi
import uk.ac.wellcome.platform.api.models.ApiConfig
import weco.api.stacks.services.WorkLookup
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiFixture
    extends ServicesFixture
    with HttpFixtures
    with IndexFixtures {

  val metricsName = "ItemsApiFixture"

  override def contextUrl =
    new URL("https://localhost/catalogue/v2/context.json")

  val apiConf =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "/catalogue",
      contextPath = "context.json"
    )

  def withItemsApi[R](index: Index)(
    testWith: TestWith[(URL, WireMockServer), R]): R =
    withSierraService {
      case (sierraServiceTest, sierraWiremockServer) =>
        val indexTest = index

        val router: ItemsApi = new ItemsApi {
          override implicit val ec: ExecutionContext = global
          override implicit val apiConfig: ApiConfig = apiConf
          override val workLookup: WorkLookup =
            WorkLookup(elasticClient)(global)
          override val sierraService: SierraService = sierraServiceTest
          override val index: Index = indexTest
        }

        withApp(router.routes) { _ =>
          testWith((router.contextUrl, sierraWiremockServer))
        }
    }
}
