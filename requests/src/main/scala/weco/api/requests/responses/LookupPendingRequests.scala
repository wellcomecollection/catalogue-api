package weco.api.requests.responses

import akka.http.scaladsl.server.Route
import weco.api.search.elasticsearch.ElasticsearchError
import weco.api.search.rest.CustomDirectives
import weco.api.requests.models.display.DisplayResultsList
import weco.api.stacks.services.{ItemLookup, ItemLookupFailure, ItemLookupSuccess, SierraService}
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait LookupPendingRequests extends CustomDirectives {
  val sierraService: SierraService
  val itemLookup: ItemLookup

  implicit val ec: ExecutionContext

  def lookupRequests(patronNumber: SierraPatronNumber): Route = {
    val itemHolds = for {
      holdsMap <- sierraService.getHolds(patronNumber)

      itemLookupResults <- itemLookup.bySourceIdentifiers(holdsMap.keys.toSeq)

      itemsFound = itemLookupResults.zip(holdsMap.keys).flatMap {
        case (ItemLookupSuccess(item, works), _) => {
          // TODO: Here is where we would apply any logic for picking the "correct" work
          val workTitle = works.collectFirst {
            case work if work.data.title.isDefined => work.data.title.get
          }

          Some((item, workTitle))
        }
        case (ItemLookupFailure(elasticError: ElasticsearchError), srcId) =>
          warn(
            s"Error looking up item ${srcId}. Elasticsearch error: ${elasticError}"
          )
          None
      }

      itemHoldTuples = itemsFound.flatMap { case (item, holdTitle) =>
        holdsMap.get(item.id.sourceIdentifier).map { hold =>
          (hold, item, holdTitle)
        }
      }

    } yield itemHoldTuples.toList

    onComplete(itemHolds) {
      case Success(value) => complete(DisplayResultsList(value))
      case Failure(err)   => failWith(err)
    }
  }
}
