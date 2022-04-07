package weco.api.items.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.scalatest.Suite
import weco.api.items.ItemsApi
import weco.api.items.services.{
  ItemUpdateService,
  SierraItemUpdater,
  WorkLookup
}
import weco.api.search.models.ApiConfig
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, MemoryHttpClient}
import weco.sierra.fixtures.SierraSourceFixture

import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiFixture extends SierraSourceFixture {
  this: Suite =>

  val metricsName = "ItemsApiFixture"

  implicit val apiConfig: ApiConfig =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "/catalogue"
    )

  def withItemsApi[R](
    catalogueResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    sierraResponses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[Unit, R]): R =
    withActorSystem { implicit actorSystem =>
      withSierraSource(sierraResponses) { sierraSource =>
        val itemsUpdaters = List(
          new SierraItemUpdater(sierraSource)
        )

        val catalogueApiClient = new MemoryHttpClient(catalogueResponses) with HttpGet {
          override val baseUri: Uri = Uri("http://catalogue:9001")
        }

        val api: ItemsApi = new ItemsApi(
          itemUpdateService = new ItemUpdateService(itemsUpdaters),
          workLookup = new WorkLookup(catalogueApiClient),
        )

        withApp(api.routes) { _ =>
          testWith(())
        }
      }
    }
}
