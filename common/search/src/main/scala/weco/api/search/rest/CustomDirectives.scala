package weco.api.search.rest

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.{Directive, Route}
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
        // akka-http uses 0 to indicate no explicit port in the URI
        .withPort(0)
        .withPath(
          Uri.Path(apiConfig.publicRootPath) ++ uri.path
        )
    }

  def elasticError(
    documentType: String,
    err: ElasticsearchError,
    usingUserSpecifiedIndex: Boolean = false
  ): Route = {
    val displayError =
      err match {

        // If a user specifies an index explicitly, e.g. /works?_index=my-great-index, and
        // that index doesn't exist, we want to return a 404 error -- they've asked for
        // something that can't be found.
        //
        // If a user doesn't specify an index, e.g. /works, and the default index doesn't exist,
        // we want to return a 500 error -- something is wrong with our defaults.
        case IndexNotFoundError(underlying) if !usingUserSpecifiedIndex =>
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
