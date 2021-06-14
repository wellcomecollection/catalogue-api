package uk.ac.wellcome.platform.api.items.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.sksamuel.elastic4s.Index
import org.scalatest.Suite
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.items.ItemsApi
import uk.ac.wellcome.platform.api.models.ApiConfig
import weco.api.stacks.services.WorkLookup
import weco.http.fixtures.HttpFixtures

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiFixture
    extends ServicesFixture
    with HttpFixtures
    with IndexFixtures { this: Suite =>

  val metricsName = "ItemsApiFixture"

  override def contextUrl =
    new URL("https://localhost/catalogue/v2/context.json")

  implicit val apiConfig: ApiConfig =
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
      case (sierraService, sierraWiremockServer) =>
        val api: ItemsApi = new ItemsApi(
          sierraService = sierraService,
          workLookup = WorkLookup(elasticClient),
          index = index
        )

        withApp(api.routes) { _ =>
          testWith((api.contextUrl, sierraWiremockServer))
        }
    }
}
