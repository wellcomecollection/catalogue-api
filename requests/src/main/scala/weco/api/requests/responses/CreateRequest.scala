package weco.api.requests.responses

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.api.stacks.models.{
  CannotBeRequested,
  HoldAccepted,
  HoldRejected,
  NoSuchUser,
  OnHoldForAnotherUser,
  UnknownError,
  UserAtHoldLimit
}
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
        val conflict = (StatusCodes.Conflict, HttpEntity.Empty)

        onComplete(result) {
          case Success(HoldAccepted) => complete(accepted)
          case Success(HoldRejected) => complete(conflict)
          case Success(UserAtHoldLimit) =>
            complete(
              StatusCodes.Forbidden ->
                ContextResponse(
                  contextUrl = contextUrl,
                  DisplayError(
                    statusCode = StatusCodes.Forbidden,
                    description =
                      "You are at your account limit and you cannot request more items"
                  )
                )
            )
          case Success(CannotBeRequested) =>
            invalidRequest("You cannot request " + itemId)
          case Success(UnknownError) =>
            internalError(
              new Throwable(s"Unknown error when requesting $itemId"))

          case Success(OnHoldForAnotherUser) =>
            complete(
              StatusCodes.Conflict ->
                ContextResponse(
                  contextUrl = contextUrl,
                  DisplayError(
                    statusCode = StatusCodes.Conflict,
                    description = s"Item $itemId is on hold for another reader"
                  )
                )
            )

          case Success(NoSuchUser(patron)) =>
            notFound(s"There is no such user $patron")

          case Failure(err) => failWith(err)
        }

      case Right(sourceIdentifier) =>
        // TODO: This looks wrong
        warn(
          s"Somebody tried to request non-Sierra item $itemId / $sourceIdentifier")
        invalidRequest("You cannot request " + itemId)

      case Left(err) => elasticError("Item", err)
    }
}
