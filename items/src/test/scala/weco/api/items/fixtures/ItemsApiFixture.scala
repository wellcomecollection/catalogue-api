package weco.api.items.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import com.sksamuel.elastic4s.Index
import org.scalatest.Suite
import weco.fixtures.TestWith
import weco.catalogue.internal_model.index.IndexFixtures
import weco.api.items.ItemsApi
import weco.api.search.models.ApiConfig
import weco.api.stacks.services.{SierraService, WorkLookup}
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiFixture extends HttpFixtures with IndexFixtures { this: Suite =>

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

  def withItemsApi[R](index: Index,
                      responses: Seq[(HttpRequest, HttpResponse)] = Seq())(
    testWith: TestWith[URL, R]): R = {

    val httpClient = new MemoryHttpClient(responses) with HttpGet
    with HttpPost {
      override val baseUri: Uri = Uri("http://sierra:1234")
    }

    withMaterializer { implicit mat =>
      val api: ItemsApi = new ItemsApi(
        sierraService = SierraService(httpClient),
        workLookup = WorkLookup(elasticClient),
        index = index
      )

      withApp(api.routes) { _ =>
        testWith(api.contextUrl)
      }
    }
  }
}
