package weco.api.requests.responses

import org.apache.pekko.http.scaladsl.server.Route
import weco.api.requests.models.display.DisplayResultsList
import weco.api.requests.services.RequestsService
import weco.http.FutureDirectives
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait LookupPendingRequests extends FutureDirectives {
  val requestsService: RequestsService

  implicit val ec: ExecutionContext

  def lookupRequests(patronNumber: SierraPatronNumber): Route =
    onComplete(requestsService.getRequests(patronNumber)) {
      case Success(value) => complete(DisplayResultsList(value))
      case Failure(err)   => internalError(err)
    }
}
