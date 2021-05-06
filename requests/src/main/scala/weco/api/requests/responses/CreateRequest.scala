package weco.api.requests.responses

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.api.stacks.models.{HoldAccepted, HoldRejected, StacksUserIdentifier}
import weco.api.stacks.services.ItemLookup
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait CreateRequest extends CustomDirectives with Logging {
  implicit val ec: ExecutionContext

  val sierraService: SierraService
  val itemLookup: ItemLookup
  val index: Index

  def createRequest(itemId: CanonicalId,
                    userIdentifier: StacksUserIdentifier): Future[Route] =
    itemLookup.byCanonicalId(itemId)(index).map {
      case Right(sourceIdentifier)
          if sourceIdentifier.identifierType == SierraSystemNumber =>
        val result = sierraService.placeHold(
          userIdentifier = userIdentifier,
          sourceIdentifier = sourceIdentifier
        )

        val accepted = (StatusCodes.Accepted, HttpEntity.Empty)
        val conflict = (StatusCodes.Conflict, HttpEntity.Empty)

        onComplete(result) {
          case Success(HoldAccepted(_)) => complete(accepted)
          case Success(HoldRejected(_)) => complete(conflict)
          case Failure(err)             => failWith(err)
        }

      case Right(sourceIdentifier) =>
        warn(
          s"Somebody tried to request non-Sierra item $itemId / $sourceIdentifier")
        invalidRequest("You cannot request " + itemId)

      case Left(err) => elasticError("Item", err)
    }
}
