package uk.ac.wellcome.platform.api.common.services.config.builders

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.typesafe.config.Config
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import weco.http.client.{AkkaHttpClient, HttpGet, HttpPost}
import weco.http.client.sierra.SierraOauthHttpClient

import scala.concurrent.ExecutionContext

object SierraServiceBuilder {
  def build(config: Config)(
    implicit
    as: ActorSystem,
    ec: ExecutionContext
  ): SierraService = {
    val username = config.requireString("sierra.api.key")
    val password = config.requireString(("sierra.api.secret"))

    val client = new AkkaHttpClient() with HttpGet with HttpPost {
      override val baseUri: Uri = Uri(
        config.requireString("sierra.api.baseUrl")
      )
    }

    val authenticatedClient = new SierraOauthHttpClient(
      client,
      credentials = BasicHttpCredentials(
        username = username,
        password = password
      )
    )

    SierraService(authenticatedClient)
  }
}
