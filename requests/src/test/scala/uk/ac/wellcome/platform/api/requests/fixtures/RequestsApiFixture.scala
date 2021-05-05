package uk.ac.wellcome.platform.api.requests.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.sksamuel.elastic4s.Index

import java.net.URL
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import weco.http.fixtures.HttpFixtures
import uk.ac.wellcome.platform.api.common.services.{
  SierraService,
  StacksService
}
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.RequestsApi
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.stacks.services.ItemLookup

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture
    extends ServicesFixture
    with HttpFixtures
    with IndexFixtures {

  val metricsName = "RequestsApiFixture"

  val contextURLTest = new URL(
    "http://api.wellcomecollection.org/requests/v1/context.json"
  )

  val apiConf =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue",
      contextPath = "context.json"
    )

  def withRequestsApi[R](index: Index)(
    testWith: TestWith[WireMockServer, R]): R =
    withStacksService {
      case (stacksService, server) =>
        val givenIndex = index

        val itLookup = new ItemLookup(
          elasticsearchService = new ElasticsearchService(elasticClient)(global)
        )(global)

        val router: RequestsApi = new RequestsApi {
          override implicit val ec: ExecutionContext = global
          override implicit val stacksWorkService: StacksService =
            stacksService
          override implicit val apiConfig: ApiConfig = apiConf

          override def context: String = contextUri

          override implicit val itemLookup: ItemLookup = itLookup
          override val index: Index = givenIndex
          override implicit val sierraService: SierraService =
            stacksService.sierraService
        }

        withApp(router.routes) { _ =>
          testWith(server)
        }
    }

}
