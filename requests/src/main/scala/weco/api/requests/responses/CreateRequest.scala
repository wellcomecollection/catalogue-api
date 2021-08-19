package weco.api.requests.responses

import akka.http.scaladsl.model.{HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import grizzled.slf4j.Logging
import weco.api.requests.services.RequestsService
import weco.api.search.elasticsearch.ElasticsearchError
import weco.api.search.rest.CustomDirectives
import weco.api.stacks.models.HoldRejected.ItemUnavailableFromSourceSystem
import weco.api.stacks.models.HoldRejected
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.http.ErrorDirectives
import weco.http.models.DisplayError
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.{ExecutionContext, Future}

trait CreateRequest extends CustomDirectives with ErrorDirectives with Logging {
  implicit val ec: ExecutionContext

  val requestsService: RequestsService

  def createRequest(
    itemId: CanonicalId,
    patronNumber: SierraPatronNumber
  ): Future[Route] = {
    requestsService.makeRequest(itemId, patronNumber) map {
      case Right(_) =>
        val accepted = (StatusCodes.Accepted, HttpEntity.Empty)
        complete(accepted)

      case Left(ItemUnavailableFromSourceSystem) =>
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
      case err: ElasticsearchError =>
        elasticError("Item", err)
      case err =>
        internalError(err)
    }
  }

  private def handleError(
    reason: HoldRejected,
    itemId: CanonicalId
  ): (StatusCode, Option[String]) =
    reason match {
      case HoldRejected.ItemCannotBeRequested =>
        (StatusCodes.BadRequest, Some(s"You cannot request $itemId"))

      case HoldRejected.ItemIsOnHoldForAnotherUser =>
        (
          StatusCodes.Conflict,
          Some(s"Item $itemId is on hold for another reader")
        )

      case HoldRejected.UserDoesNotExist(patron) =>
        (StatusCodes.NotFound, Some(s"There is no such user $patron"))

      case HoldRejected.UserIsAtHoldLimit =>
        (
          StatusCodes.Forbidden,
          Some(
            "You are at your account limit and you cannot request more items"
          )
        )

      case _ =>
        (StatusCodes.InternalServerError, None)
    }
}
