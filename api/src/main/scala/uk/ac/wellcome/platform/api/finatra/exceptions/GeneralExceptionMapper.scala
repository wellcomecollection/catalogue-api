package uk.ac.wellcome.platform.api.finatra.exceptions

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.{ApiConfig, DisplayError, Error, ErrorVariant}
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class GeneralExceptionMapper @Inject()(response: ResponseBuilder,
                                       apiConfig: ApiConfig)
    extends ExceptionMapper[Exception]
    with Logging {

  override def toResponse(request: Request, exception: Exception): Response = {

    val version = getVersion(request, apiPrefix = apiConfig.pathPrefix)
    val context = buildContextUri(apiConfig = apiConfig, version = version)

    error(
      s"Sending HTTP 500 from GeneralExceptionMapper. Request: $request. Referrer ${request.referer}",
      exception)
    val result = DisplayError(
      Error(
        variant = ErrorVariant.http500,
        description = None
      ))
    val errorResponse = ResultResponse(context = context, result = result)
    response.internalServerError.json(errorResponse)
  }
}
