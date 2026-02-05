package weco.api.search.models

import org.apache.pekko.http.scaladsl.model.Uri
import com.typesafe.config.Config
import weco.typesafe.config.builders.EnrichConfig.RichConfig

case class ApiConfig(
  publicScheme: String,
  publicHost: String,
  publicRootPath: String,
  defaultPageSize: Int,
  semanticConfig: Option[SemanticConfig] = None
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
      semanticConfig = for {
          modelId <- config.getStringOption("es.semantic.modelId")
          vectorType <- config.getStringOption("es.semantic.vectorType").flatMap {
            case "dense"  => Some(VectorType.Dense)
            case "sparse" => Some(VectorType.Sparse)
            case _        => None
          }
        } yield SemanticConfig(modelId, vectorType)
    )

  def apply(
    publicRootUri: Uri,
    defaultPageSize: Int,
    semanticConfig: Option[SemanticConfig]
  ): ApiConfig =
    ApiConfig(
      publicHost = publicRootUri.authority.host.address,
      publicScheme = publicRootUri.scheme,
      publicRootPath = publicRootUri.path.toString,
      defaultPageSize = defaultPageSize,
      semanticConfig = semanticConfig
    )

  def apply(
    publicRootUri: Uri,
    defaultPageSize: Int
  ): ApiConfig =
    ApiConfig(
      publicRootUri = publicRootUri,
      defaultPageSize = defaultPageSize,
      semanticConfig = None
    )
}
