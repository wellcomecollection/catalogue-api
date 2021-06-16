package weco.api.requests.responses

import akka.http.scaladsl.model.{HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.api.stacks.models.HoldRejected
import weco.api.stacks.services.ItemLookup
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber
import weco.http.ErrorDirectives
import weco.http.models.{ContextResponse, DisplayError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait CreateRequest extends CustomDirectives with ErrorDirectives with Logging {
  implicit val ec: ExecutionContext

  val sierraService: SierraService
  val itemLookup: ItemLookup

  def createRequest(itemId: CanonicalId,
                    patronNumber: SierraPatronNumber): Future[Route] =
    itemLookup.byCanonicalId(itemId).map {
      case Right(item)
          if item.id.sourceIdentifier.identifierType == SierraSystemNumber =>
        val result = sierraService.placeHold(
          patron = patronNumber,
          sourceIdentifier = item.id.sourceIdentifier
        )

        val accepted = (StatusCodes.Accepted, HttpEntity.Empty)

        onComplete(result) {
          case Success(Right(_)) => complete(accepted)

          case Success(Left(holdRejected)) =>
            val (status, description) =
              handleError(holdRejected, itemId = itemId)
            complete(
              status ->
                ContextResponse(
                  contextUrl = contextUrl,
                  DisplayError(
                    errorType = "http",
                    httpStatus = status.intValue,
                    label = status.reason(),
                    description = description
                  )
                )
            )

          case Failure(err) => failWith(err)
        }

      case Right(sourceIdentifier) =>
        // TODO: This looks wrong
        warn(
          s"Somebody tried to request non-Sierra item $itemId / $sourceIdentifier")
        invalidRequest("You cannot request " + itemId)

      case Left(err) => elasticError("Item", err)
    }

  private def handleError(reason: HoldRejected,
                          itemId: CanonicalId): (StatusCode, Option[String]) =
    reason match {
      case HoldRejected.ItemCannotBeRequested =>
        (StatusCodes.BadRequest, Some(s"You cannot request $itemId"))

      case HoldRejected.ItemIsOnHoldForAnotherUser =>
        (
          StatusCodes.Conflict,
          Some(s"Item $itemId is on hold for another reader"))

      case HoldRejected.UserDoesNotExist(patron) =>
        (StatusCodes.NotFound, Some(s"There is no such user $patron"))

      case HoldRejected.UserIsAtHoldLimit =>
        (
          StatusCodes.Forbidden,
          Some(
            "You are at your account limit and you cannot request more items"))

      case _ =>
        (StatusCodes.InternalServerError, None)
    }
}
