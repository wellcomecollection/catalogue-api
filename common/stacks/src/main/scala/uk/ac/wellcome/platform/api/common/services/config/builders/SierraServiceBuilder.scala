package uk.ac.wellcome.platform.api.common.services.config.builders

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.typesafe.config.Config
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import weco.api.stacks.http.SierraOauthHttpClient
import weco.api.stacks.http.impl.AkkaHttpClient

import scala.concurrent.ExecutionContext

object SierraServiceBuilder {
  def build(config: Config)(
    implicit
    as: ActorSystem,
    ec: ExecutionContext
  ): SierraService = {
    val username = config.requireString("sierra.api.key")
    val password = config.requireString(("sierra.api.secret"))
    val baseUrl = config.requireString("sierra.api.baseUrl")

    val client = new AkkaHttpClient(baseUri = Uri(baseUrl))

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
