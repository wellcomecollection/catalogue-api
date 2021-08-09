package weco.api.requests.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import weco.api.requests.RequestsApi
import weco.api.search.models.ApiConfig
import weco.api.stacks.fixtures.SierraServiceFixture
import weco.api.stacks.services.ItemLookup
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
    itemLookup: ItemLookup,
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[Unit, R]): R =
    withMaterializer { implicit mat =>
      withSierraService(responses) { sierraService =>
        val api: RequestsApi = new RequestsApi(
          sierraService = sierraService,
          itemLookup = itemLookup
        )

        withApp(api.routes) { _ =>
          testWith(())
        }
      }
    }
}
