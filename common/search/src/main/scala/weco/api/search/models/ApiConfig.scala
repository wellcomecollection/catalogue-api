package weco.api.search.models

import org.apache.pekko.http.scaladsl.model.Uri
import com.typesafe.config.Config
import weco.typesafe.config.builders.EnrichConfig.RichConfig

case class ApiConfig(
  publicScheme: String,
  publicHost: String,
  publicRootPath: String,
  defaultPageSize: Int,
) {
  // Used to determine whether we're running in a dev environment
  def environment: ApiEnvironment = publicHost match {
    case _ if publicHost.contains("api-dev")   => ApiEnvironment.Dev
    case _ if publicHost.contains("api-stage") => ApiEnvironment.Stage
    case _                                     => ApiEnvironment.Prod
  }
}

sealed trait ApiEnvironment
object ApiEnvironment {
  case object Dev extends ApiEnvironment
  case object Stage extends ApiEnvironment
  case object Prod extends ApiEnvironment
}

object ApiConfig {
  private val defaultRootUri = "https://api.wellcomecollection.org/catalogue/v2"

  def build(config: Config): ApiConfig =
    ApiConfig(
      publicRootUri = Uri(
        config
          .getStringOption("api.public-root")
          .getOrElse(defaultRootUri)
      ),
      defaultPageSize = config
        .getIntOption("api.pageSize")
        .getOrElse(10),
    )

  def apply(
    publicRootUri: Uri,
    defaultPageSize: Int,
  ): ApiConfig =
    ApiConfig(
      publicHost = publicRootUri.authority.host.address,
      publicScheme = publicRootUri.scheme,
      publicRootPath = publicRootUri.path.toString,
      defaultPageSize = defaultPageSize,
    )
}
