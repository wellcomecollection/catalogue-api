package uk.ac.wellcome.platform.api.rest

import akka.http.scaladsl.model.{StatusCodes, Uri}
import uk.ac.wellcome.platform.api.models._
import akka.http.scaladsl.server.{Directive, Directives, Route}
import com.sksamuel.elastic4s.ElasticError
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import grizzled.slf4j.Logger
import io.circe.Printer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.platform.api.elasticsearch.ElasticsearchErrorHandler
import weco.http.json.DisplayJsonUtil
import weco.http.models.{ContextResponse, DisplayError}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait CustomDirectives extends Directives with FailFastCirceSupport {
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

  def gone(description: String): Route =
    error(
      DisplayError(statusCode = StatusCodes.Gone, description = description)
    )

  def notFound(description: String): Route =
    error(
      DisplayError(statusCode = StatusCodes.NotFound, description = description)
    )

  def invalidRequest(description: String): Route =
    error(
      DisplayError(
        statusCode = StatusCodes.BadRequest,
        description = description)
    )

  def internalError(err: Throwable): Route = {
    logger.error(s"Sending HTTP 500: $err", err)
    error(DisplayError(statusCode = StatusCodes.InternalServerError))
  }

  def getWithFuture(future: Future[Route]): Route =
    get {
      onComplete(future) {
        case Success(resp) => resp
        case Failure(err)  => internalError(err)
      }
    }

  private def error(err: DisplayError): Route = {
    val status = err.httpStatus
    complete(
      status -> ContextResponse(context = contextUri, result = err)
    )
  }

  implicit val jsonPrinter: Printer = DisplayJsonUtil.printer

  private lazy val logger = Logger(this.getClass.getName)
}
