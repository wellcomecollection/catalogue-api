package weco.api.requests.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.sksamuel.elastic4s.{ElasticClient, Index}
import weco.api.requests.RequestsApi
import weco.api.requests.services.{ItemLookup, RequestsService}
import weco.api.search.models.ApiConfig
import weco.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture extends SierraServiceFixture {

  val metricsName = "RequestsApiFixture"

  implicit val apiConfig: ApiConfig =
    ApiConfig(
      publicHost = "localhost/",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue"
    )

  def withRequestsApi[R](
                          elasticClient: ElasticClient,
                          index: Index,
                          responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[Unit, R]): R =
    withSierraService(responses) { sierraService =>

      val requestsService = new RequestsService(
        sierraService = sierraService,
        itemLookup = ItemLookup(elasticClient, index = index)
      )

      val api: RequestsApi = new RequestsApi(requestsService)

      withApp(api.routes) { _ =>
        testWith(())
      }
    }
}
