package uk.ac.wellcome.platform.api.common.fixtures

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.github.tomakehurst.wiremock.WireMockServer
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.services.SierraService
import weco.api.stacks.http.SierraOauthHttpClient
import weco.api.stacks.http.impl.AkkaHttpClient

import scala.concurrent.ExecutionContext.Implicits.global

trait ServicesFixture extends SierraWireMockFixture with Akka {
  def withSierraService[R](
    testWith: TestWith[(SierraService, WireMockServer), R]
  ): R = {
    withMockSierraServer {
      case (sierraApiUrl, wireMockServer) =>
        withActorSystem { implicit as =>
          val client = new AkkaHttpClient(
            baseUri = Uri(f"$sierraApiUrl/iii/sierra-api")
          )

          val authenticatedClient = new SierraOauthHttpClient(
            client, credentials = BasicHttpCredentials("username", "password")
          )

          testWith(
            (
              SierraService(authenticatedClient),
              wireMockServer
            )
          )
        }
    }
  }
}
