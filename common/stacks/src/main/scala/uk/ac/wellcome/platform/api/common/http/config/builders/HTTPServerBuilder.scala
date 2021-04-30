package uk.ac.wellcome.platform.api.common.http.config.builders

import java.net.URL

import com.typesafe.config.Config
import uk.ac.wellcome.platform.api.common.http.config.models.HTTPServerConfig
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object HTTPServerBuilder {
  def buildHTTPServerConfig(config: Config): HTTPServerConfig = {
    HTTPServerConfig(
      host = config.requireString("http.host"),
      port = config.requireInt("http.port")
    )
  }
}
