package weco.api.requests.responses

import akka.http.scaladsl.server.Route
import weco.api.search.rest.CustomDirectives
import weco.api.stacks.models.display.DisplayResultsList
import weco.api.stacks.services.{ItemLookup, SierraService}
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LookupPendingRequests extends CustomDirectives {
  val sierraService: SierraService
  val itemLookup: ItemLookup

  implicit val ec: ExecutionContext

  def lookupRequests(patronNumber: SierraPatronNumber): Route = {
    val itemHolds = for {
      holdsMap <- sierraService.getHolds(patronNumber)

      itemLookupResults <- Future.sequence(
        holdsMap.keys.map(itemLookup.bySourceIdentifier)
          .map(_.map {
            case Right(item) => Some(item)
            case Left(err) =>
              warn(s"$err")
              None
          })
          .map(_.recover {
            case err =>
              warn(s"$err")
              None
          })
      ).map(
        _.flatten
      )

      itemHoldTuples = itemLookupResults.flatMap { item =>
        holdsMap.get(item.id.sourceIdentifier).map { hold =>
          (hold, item)
        }
      } toList

    } yield itemHoldTuples

    onComplete(itemHolds) {
      case Success(value) => complete(DisplayResultsList(value))
      case Failure(err)   => failWith(err)
    }
  }
}
