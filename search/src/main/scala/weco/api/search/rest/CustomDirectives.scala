package weco.api.search.rest

import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.server.{Directive, Route}
import weco.api.search.elasticsearch.{
  ElasticsearchError,
  ElasticsearchErrorHandler,
  IndexNotFoundError,
  UnknownError
}
import weco.api.search.models.ApiConfig
import weco.http.FutureDirectives

trait CustomDirectives extends FutureDirectives {
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
        // pekko-http uses 0 to indicate no explicit port in the URI
        .withPort(0)
        .withPath(
          Uri.Path(apiConfig.publicRootPath) ++ uri.path
        )
    }

  def elasticError(documentType: String, err: ElasticsearchError): Route = {
    val displayError =
      err match {

        // If the default index doesn't exist, we want to return a 500 error,
        // not a 404 -- something is wrong with our defaults.
        case IndexNotFoundError(underlying) =>
          ElasticsearchErrorHandler.buildDisplayError(
            documentType,
            e = UnknownError(underlying)
          )

        case otherError =>
          ElasticsearchErrorHandler.buildDisplayError(
            documentType,
            e = otherError
          )
      }

    complete(displayError.httpStatus -> displayError)
  }
}
