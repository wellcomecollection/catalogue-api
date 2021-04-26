package uk.ac.wellcome.platform.stacks.common.http

import akka.http.scaladsl.model.StatusCodes

import java.net.URL
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.ExceptionHandler
import grizzled.slf4j.Logging
import weco.http.models.{ContextResponse, DisplayError}

trait WellcomeExceptionHandler extends Logging {
  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._

  val httpMetrics: HttpMetrics
  val contextURL: URL

  implicit val exceptionHandler: ExceptionHandler = buildExceptionHandler()

  private def buildExceptionHandler(): ExceptionHandler =
    ExceptionHandler {
      case err: Exception =>
        logger.error(s"Unexpected exception $err")
        val error = ContextResponse(
          context = contextURL.toString,
          DisplayError(statusCode = StatusCodes.InternalServerError)
        )
        httpMetrics.sendMetricForStatus(InternalServerError)
        complete(InternalServerError -> error)
    }
}
