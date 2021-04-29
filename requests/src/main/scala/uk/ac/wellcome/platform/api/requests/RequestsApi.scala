package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Printer
import uk.ac.wellcome.platform.api.common.models.display.DisplayResultsList
import uk.ac.wellcome.platform.api.common.models.StacksUserIdentifier
import uk.ac.wellcome.platform.api.common.services.{HoldAccepted, HoldRejected, StacksService}
import uk.ac.wellcome.platform.api.requests.models.ItemRequest
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait RequestsApi extends FailFastCirceSupport {

  import akka.http.scaladsl.server.Directives._
  import io.circe.generic.auto._

  implicit val ec: ExecutionContext
  implicit val stacksWorkService: StacksService

  // Omit optional fields in response to clients
  implicit val printer: Printer =
    Printer.noSpaces.copy(dropNullValues = true)

  val routes: Route = concat(
    pathPrefix("users" / Segment / "item-requests") { userId: String =>
      val userIdentifier = StacksUserIdentifier(userId)

      post {
        entity(as[ItemRequest]) {
          itemRequest: ItemRequest =>
            val canonicalId =
              CanonicalId(itemRequest.itemId)

            val result = stacksWorkService.requestHoldOnItem(
              userIdentifier = userIdentifier,
              itemId = canonicalId,
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
      } ~ get {
        val result = stacksWorkService.getStacksUserHolds(userIdentifier)

        onComplete(result) {
          case Success(value) => complete(DisplayResultsList(value))
          case Failure(err)   => failWith(err)
        }
      }
    }
  )
}
