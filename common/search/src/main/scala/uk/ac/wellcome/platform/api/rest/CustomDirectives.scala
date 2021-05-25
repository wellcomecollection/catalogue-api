package uk.ac.wellcome.platform.api.rest

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.{Directive, Route}
import uk.ac.wellcome.platform.api.elasticsearch.ElasticsearchErrorHandler
import uk.ac.wellcome.platform.api.models.ApiConfig
import weco.api.search.elasticsearch.ElasticsearchError
import weco.http.FutureDirectives
import weco.http.models.{ContextResponse, DisplayError}

import java.net.URL

trait CustomDirectives extends FutureDirectives {
  import weco.http.models.ContextResponse._

  implicit val apiConfig: ApiConfig

  // Directive for getting public URI of the current request, using the host
  // and scheme as per the config.
  // (without this URIs end up looking like https://localhost:8888/..., rather
  // than https://api.wellcomecollection.org/...))
  def extractPublicUri: Directive[Tuple1[Uri]] =
    extractUri.map { uri =>
      uri
        .withScheme(apiConfig.publicScheme)
        .withHost(apiConfig.publicHost)
        // akka-http uses 0 to indicate no explicit port in the URI
        .withPort(0)
        .withPath(
          Uri.Path(apiConfig.publicRootPath) ++ uri.path
        )
    }

  def contextUrl: URL =
    apiConfig match {
      case ApiConfig(scheme, host, rootPath, contextPath, _)
          if rootPath.isEmpty =>
        new URL(s"$scheme://$host/$contextPath")
      case ApiConfig(scheme, host, rootPath, contextPath, _) =>
        new URL(s"$scheme://$host$rootPath/$contextPath")
    }

  def elasticError(documentType: String, err: ElasticsearchError): Route =
    error(
      ElasticsearchErrorHandler.buildDisplayError(documentType, err)
    )

  private def error(err: DisplayError): Route = {
    val status = err.httpStatus
    complete(
      status -> ContextResponse(contextUrl = contextUrl, result = err)
    )
  }
}
