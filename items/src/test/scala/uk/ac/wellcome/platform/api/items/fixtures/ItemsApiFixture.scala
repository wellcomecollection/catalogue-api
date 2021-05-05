package uk.ac.wellcome.platform.api.items.fixtures

import java.net.URL
import com.sksamuel.elastic4s.Index
import com.github.tomakehurst.wiremock.WireMockServer
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.items.ItemsApi
import uk.ac.wellcome.platform.api.models.ApiConfig
import weco.api.search.elasticsearch.ElasticsearchService
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext

trait ItemsApiFixture extends ServicesFixture with HttpFixtures with IndexFixtures {

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

  val esService = new ElasticsearchService(elasticClient)

  def withItemsApi[R](index: Index)(testWith: TestWith[WireMockServer, R]): R =
    withStacksService {
      case (stacksService, server) =>
        val givenIndex = index

        val eContext = ec

        val router: ItemsApi = new ItemsApi {
          override implicit lazy val ec: ExecutionContext = eContext
          override implicit val stacksWorkService: StacksService =
            stacksService
          override implicit val apiConfig: ApiConfig = apiConf

          override def context: String = contextUri

          override lazy val elasticsearchService: ElasticsearchService = esService

          override val index: Index = givenIndex
        }

        withApp(router.routes) { _ =>
          testWith(server)
        }
    }
}
