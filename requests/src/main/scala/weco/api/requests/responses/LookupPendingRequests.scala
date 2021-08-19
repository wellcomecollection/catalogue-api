package weco.api.requests.responses

import akka.http.scaladsl.server.Route
import weco.api.search.rest.CustomDirectives
import weco.api.requests.models.display.DisplayResultsList
import weco.api.requests.services.RequestsService
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait LookupPendingRequests extends CustomDirectives {
  val requestsService: RequestsService

  implicit val ec: ExecutionContext

  def lookupRequests(patronNumber: SierraPatronNumber): Route = {
    val itemHolds = for {
      itemHoldTuples <- requestsService.getRequests(patronNumber)
    } yield itemHoldTuples

    onComplete(itemHolds) {
      case Success(value) => complete(DisplayResultsList(value))
      case Failure(err)   => failWith(err)
    }
  }
}
