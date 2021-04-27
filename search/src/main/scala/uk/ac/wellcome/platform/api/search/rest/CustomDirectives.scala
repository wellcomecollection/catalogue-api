package uk.ac.wellcome.platform.api.search.rest

import akka.http.scaladsl.model.Uri
import uk.ac.wellcome.platform.api.search.models._
import akka.http.scaladsl.server.{Directive, Route}
import com.sksamuel.elastic4s.ElasticError
import uk.ac.wellcome.api.display.models.ApiVersions
import uk.ac.wellcome.platform.api.search.elasticsearch.ElasticsearchErrorHandler
import weco.http.FutureDirectives
import weco.http.models.{ContextResponse, DisplayError}

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
        .withHost(apiConfig.host)
        // akka-http uses 0 to indicate no explicit port in the URI
        .withPort(0)
        .withScheme(apiConfig.scheme)
    }

  def contextUri: String =
    apiConfig match {
      case ApiConfig(host, scheme, _, pathPrefix, contextSuffix) =>
        s"$scheme://$host/$pathPrefix/${ApiVersions.v2}/$contextSuffix"
    }

  def elasticError(err: ElasticError): Route =
    error(
      ElasticsearchErrorHandler.buildDisplayError(err)
    )

  private def error(err: DisplayError): Route = {
    val status = err.httpStatus
    complete(
      status -> ContextResponse(context = contextUri, result = err)
    )
  }
}
