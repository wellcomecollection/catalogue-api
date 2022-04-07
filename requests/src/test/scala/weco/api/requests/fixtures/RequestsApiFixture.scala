package weco.api.requests.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.scalatest.Suite
import weco.api.requests.RequestsApi
import weco.api.requests.services.RequestsService
import weco.api.search.models.ApiConfig
import weco.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait RequestsApiFixture extends SierraServiceFixture with ItemLookupFixture {
  this: Suite =>

  val metricsName = "RequestsApiFixture"

  implicit val apiConfig: ApiConfig =
    ApiConfig(
      publicHost = "localhost/",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "catalogue"
    )

  def withRequestsApi[R](
    sierraResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    catalogueResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
  )(testWith: TestWith[Unit, R]): R =
    withSierraService(sierraResponses) { sierraService =>
      withItemLookup(catalogueResponses) { itemLookup =>
        val requestsService = new RequestsService(
          sierraService = sierraService,
          itemLookup = itemLookup
        )

        val api: RequestsApi = new RequestsApi(requestsService)

        withApp(api.routes) { _ =>
          testWith(())
        }
      }
    }
}
