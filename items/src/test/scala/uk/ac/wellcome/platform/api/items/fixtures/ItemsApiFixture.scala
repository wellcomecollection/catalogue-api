package uk.ac.wellcome.platform.api.items.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import java.net.URL

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.platform.api.common.fixtures.{
  HttpFixtures,
  ServicesFixture
}
import uk.ac.wellcome.platform.api.common.http.WellcomeHttpApp
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.items.ItemsApi
import uk.ac.wellcome.platform.api.models.ApiConfig
import weco.http.monitoring.HttpMetrics

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiFixture extends ServicesFixture with HttpFixtures {

  val metricsName = "ItemsApiFixture"

  val contextURLTest = new URL(
    "http://api.wellcomecollection.org/stacks/v1/context.json"
  )

  val apiConf =
    ApiConfig(
      host = "localhost",
      scheme = "https",
      defaultPageSize = 10,
      pathPrefix = "catalogue",
      contextSuffix = "context.json"
    )

  def withApp[R](testWith: TestWith[WireMockServer, R]): R =
    withActorSystem { implicit actorSystem =>
      val httpMetrics = new HttpMetrics(
        name = metricsName,
        metrics = new MemoryMetrics()
      )

      withStacksService {
        case (stacksService, server) =>
          val router: ItemsApi = new ItemsApi {
            override implicit val ec: ExecutionContext = global
            override implicit val stacksWorkService: StacksService =
              stacksService
            override implicit val apiConfig: ApiConfig = apiConf

            override def context: String = contextUri
          }

          val app = new WellcomeHttpApp(
            routes = router.routes,
            httpMetrics = httpMetrics,
            httpServerConfig = httpServerConfigTest,
            contextURL = contextURLTest,
            appName = metricsName
          )

          app.run()

          testWith(server)
      }
    }
}
