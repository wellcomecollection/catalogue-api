package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.server.Route
import uk.ac.wellcome.platform.api.common.models.StacksUserIdentifier
import uk.ac.wellcome.platform.api.common.models.display.DisplayResultsList
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.requests.models.ItemRequest
import uk.ac.wellcome.platform.api.requests.responses.CreateHold

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait RequestsApi extends CreateHold {
  implicit val ec: ExecutionContext
  implicit val stacksWorkService: StacksService

  val routes: Route = concat(
    pathPrefix("users" / Segment / "item-requests") { userId: String =>
      post {
        entity(as[ItemRequest]) { itemRequest: ItemRequest =>
          createHold(userId, itemRequest)
        }
      } ~ get {
        val userIdentifier = StacksUserIdentifier(userId)
        val result = stacksWorkService.getStacksUserHolds(userIdentifier)

        onComplete(result) {
          case Success(value) => complete(DisplayResultsList(value))
          case Failure(err)   => failWith(err)
        }
      }
    }
  )
}
