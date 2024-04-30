package weco.api.search.models

import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import weco.typesafe.config.builders.EnrichConfig.RichConfig

case class ApiConfig(
  publicScheme: String,
  publicHost: String,
  publicRootPath: String,
  defaultPageSize: Int,
  // Used to determine whether we're running in a dev environment
  isDev: Boolean
)

object ApiConfig {
  private val defaultRootUri = "https://api.wellcomecollection.org/catalogue/v2"

  def build(config: Config): ApiConfig = {
    val apiPublicRoot = config
      .getStringOption("api.public-root")
      .getOrElse(defaultRootUri)

    ApiConfig(
      publicRootUri = Uri(apiPublicRoot),
      defaultPageSize = config
        .getIntOption("api.pageSize")
        .getOrElse(10),
      // Infer whether we're running in a dev environment from the public root
      isDev = apiPublicRoot.contains("localhost")
    )
  }

  def apply(
    publicRootUri: Uri,
    defaultPageSize: Int,
    isDev: Boolean = false
  ): ApiConfig =
    ApiConfig(
      publicHost = publicRootUri.authority.host.address,
      publicScheme = publicRootUri.scheme,
      publicRootPath = publicRootUri.path.toString,
      defaultPageSize = defaultPageSize,
      isDev = isDev
    )
}
