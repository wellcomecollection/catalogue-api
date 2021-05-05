package uk.ac.wellcome.platform.api.requests.responses

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.platform.api.common.models.StacksUserIdentifier
import uk.ac.wellcome.platform.api.common.services.{
  HoldAccepted,
  HoldRejected,
  StacksService
}
import uk.ac.wellcome.platform.api.requests.models.ItemRequest
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.api.stacks.services.ItemLookup
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState}
import weco.catalogue.internal_model.work.Item

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait CreateHold extends CustomDirectives {
  implicit val ec: ExecutionContext

  implicit val stacksWorkService: StacksService
  implicit val itemLookup: ItemLookup

  val index: Index

  def createHold(userId: String, itemRequest: ItemRequest): Route = {
    val userIdentifier = StacksUserIdentifier(userId)

    Try { CanonicalId(itemRequest.itemId) } match {
      case Success(itemId) =>
        withFuture {
          itemLookup.byCanonicalId(itemId)(index).map {
            case Right(Some(item: Item[IdState.Identified])) =>
              placeHoldOnItem(userIdentifier, item)

            case Right(None) =>
              notFound(s"Item not found for identifier ${itemRequest.itemId}")

            case Left(err) =>
              elasticError(err)
          }
        }

      case _ => notFound(s"Item not found for identifier ${itemRequest.itemId}")
    }
  }

  private def placeHoldOnItem(userIdentifier: StacksUserIdentifier,
                              item: Item[IdState.Identified]): Route = {
    val result = stacksWorkService.requestHoldOnItem(
      userIdentifier = userIdentifier,
      item = item,
      neededBy = None
    )

    val accepted = (StatusCodes.Accepted, HttpEntity.Empty)
    val conflict = (StatusCodes.Conflict, HttpEntity.Empty)

    onComplete(result) {
      case Success(HoldAccepted(_)) => complete(accepted)
      case Success(HoldRejected(_)) => complete(conflict)
      case Failure(err)             => failWith(err)
    }
  }
}
