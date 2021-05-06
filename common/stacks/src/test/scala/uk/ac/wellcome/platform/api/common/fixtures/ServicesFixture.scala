package uk.ac.wellcome.platform.api.common.fixtures

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.github.tomakehurst.wiremock.WireMockServer
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.services.source.AkkaSierraSource
import uk.ac.wellcome.platform.api.common.services.{
  CatalogueService,
  SierraService
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.ExecutionContext.Implicits.global

trait ServicesFixture
    extends AkkaCatalogueSourceFixture
    with SierraWireMockFixture {

  def withCatalogueService[R](
    testWith: TestWith[CatalogueService, R]
  ): R =
    withAkkaCatalogueSource { catalogueSource =>
      testWith(new CatalogueService(catalogueSource))
    }

  def withSierraService[R](
    testWith: TestWith[(SierraService, WireMockServer), R]
  ): R = {
    withMockSierraServer {
      case (sierraApiUrl, wireMockServer) =>
        withActorSystem { implicit as =>
          implicit val ec: ExecutionContextExecutor = as.dispatcher

          withMaterializer { _ =>
            testWith(
              (
                new SierraService(
                  new AkkaSierraSource(
                    baseUri = Uri(f"$sierraApiUrl/iii/sierra-api"),
                    credentials = BasicHttpCredentials("username", "password")
                  )
                ),
                wireMockServer
              )
            )
          }
        }
    }
  }
}
