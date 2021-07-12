package weco.api.items.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.sksamuel.elastic4s.Index
import org.scalatest.Suite
import weco.api.items.ItemsApi
import weco.api.items.services.ItemUpdateService
import weco.api.search.models.ApiConfig
import weco.api.stacks.services.WorkLookup
import weco.catalogue.internal_model.index.IndexFixtures
import weco.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiFixture extends SierraServiceFixture with IndexFixtures { this: Suite =>

  val metricsName = "ItemsApiFixture"

  implicit val apiConfig: ApiConfig =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "/catalogue"
    )

  def withItemsApi[R](
    index: Index,
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[Unit, R]): R = {
      withMaterializer { implicit mat =>
        withSierraService(responses) { sierraService =>

        val api: ItemsApi = new ItemsApi(
          itemUpdateService = new ItemUpdateService(sierraService),
          workLookup = WorkLookup(elasticClient),
          index = index
        )

        withApp(api.routes) { _ =>
          testWith(())
        }
      }
    }
  }
}
