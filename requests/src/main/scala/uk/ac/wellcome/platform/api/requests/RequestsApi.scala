package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.server.Route
import uk.ac.wellcome.platform.api.common.models.display.DisplayResultsList
import uk.ac.wellcome.platform.api.common.models.StacksUserIdentifier
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.requests.models.ItemRequest
import weco.api.requests.responses.CreateHold
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait RequestsApi extends CreateHold {
  implicit val ec: ExecutionContext
  implicit val stacksWorkService: StacksService

  val routes: Route = concat(
    pathPrefix("users" / Segment / "item-requests") { userId: String =>
      val userIdentifier = StacksUserIdentifier(userId)

      post {
        entity(as[ItemRequest]) {
          itemRequest: ItemRequest =>
            // TODO: We get the work ID as part of the item request, although right now
            // it's only for future-proofing, in case it's useful later.
            // Should we query based on the work ID?
            Try { CanonicalId(itemRequest.itemId) } match {
              case Success(itemId) =>
                withFuture {
                  createHold(itemId = itemId, userIdentifier = userIdentifier)
                }

              case _ => notFound(s"Item not found for identifier ${itemRequest.itemId}")
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
