package weco.api.requests.responses

import akka.http.scaladsl.model.{HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import grizzled.slf4j.Logging
import weco.api.requests.models.HoldRejected
import weco.api.requests.services.RequestsService
import HoldRejected.SourceSystemNotSupported
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.http.ErrorDirectives
import weco.http.models.DisplayError
import weco.sierra.models.identifiers.SierraPatronNumber
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait CreateRequest extends ErrorDirectives with Logging {
  implicit val ec: ExecutionContext

  val requestsService: RequestsService

  def createRequest(
    itemId: CanonicalId,
    pickupDate: Option[LocalDate],
    patronNumber: SierraPatronNumber
  ): Future[Route] =
    requestsService.makeRequest(itemId, pickupDate, patronNumber) map {
      case Right(_) =>
        val accepted = (StatusCodes.Accepted, HttpEntity.Empty)
        complete(accepted)

      case Left(SourceSystemNotSupported) =>
        invalidRequest("You cannot request " + itemId)

      case Left(holdRejected) =>
        val (status, description) =
          handleError(holdRejected, itemId = itemId)

        complete(
          status ->
            DisplayError(
              errorType = "http",
              httpStatus = status.intValue,
              label = status.reason(),
              description = description
            )
        )
    } recover {
      case err => internalError(err)
    }

  private def handleError(
    reason: HoldRejected,
    itemId: CanonicalId
  ): (StatusCode, Option[String]) =
    reason match {
      case HoldRejected.UserIsSelfRegistered =>
        (
          StatusCodes.Forbidden,
          Some(
            s"Your account needs to be upgraded before you can make requests. Please contact Library Enquiries (library@wellcomecollection.org)."
          )
        )

      case HoldRejected.ItemCannotBeRequested =>
        (StatusCodes.BadRequest, Some(s"You can't request $itemId"))

      case HoldRejected.ItemIsOnHoldForAnotherUser =>
        (
          StatusCodes.Conflict,
          Some(s"Item $itemId is on hold for another library member")
        )

      case HoldRejected.UserDoesNotExist(patron) =>
        (StatusCodes.NotFound, Some(s"There is no such user $patron"))

      case HoldRejected.UserIsAtHoldLimit =>
        (
          StatusCodes.Forbidden,
          Some(
            "You're at your account limit and you cannot request more items"
          )
        )

      case HoldRejected.UserAccountHasExpired =>
        (
          StatusCodes.Forbidden,
          Some(
            "Your account has expired, and you're no longer able to request items. To renew your account, please contact Library Enquiries (library@wellcomecollection.org)."
          )
        )

      case HoldRejected.ItemDoesNotExist =>
        (
          StatusCodes.NotFound,
          Some(s"Item not found for identifier $itemId")
        )

      case _ =>
        (StatusCodes.InternalServerError, None)
    }
}
