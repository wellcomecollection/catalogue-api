package uk.ac.wellcome.platform.api.models

import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig.RichConfig

case class ApiConfig(
  host: String,
  scheme: String,
  defaultPageSize: Int,
  pathPrefix: String,
  contextSuffix: String
)

object ApiConfig {
  private val defaultRootUri = "https://api.wellcomecollection.org/catalogue/v2"

  def build(config: Config): ApiConfig =
    ApiConfig(
      rootUri = Uri(
        config
          .getStringOption("api.public-root")
          .getOrElse(defaultRootUri)
      ),
      defaultPageSize = config
        .getIntOption("api.pageSize")
        .getOrElse(10),
      contextSuffix = config
        .getStringOption("api.context.suffix")
        .getOrElse("context.json")
    )

  def apply(
    rootUri: Uri,
    defaultPageSize: Int,
    contextSuffix: String
  ): ApiConfig =
    ApiConfig(
      host = rootUri.authority.host.address,
      scheme = rootUri.scheme,
      defaultPageSize = defaultPageSize,
      pathPrefix = if (rootUri.path.startsWithSlash) {
        rootUri.path.toString.drop(1)
      } else {
        rootUri.path.toString
      },
      contextSuffix = contextSuffix
    )
}
