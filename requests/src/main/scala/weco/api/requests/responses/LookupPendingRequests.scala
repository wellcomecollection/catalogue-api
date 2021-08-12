package weco.api.requests.responses

import akka.http.scaladsl.server.Route
import weco.api.search.elasticsearch.ElasticsearchError
import weco.api.stacks.models.display.DisplayResultsList
import weco.api.search.rest.CustomDirectives
import weco.api.stacks.models.StacksHold
import weco.api.stacks.services.{ItemLookup, SierraService}
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.Item
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LookupPendingRequests extends CustomDirectives {
  val sierraService: SierraService
  val itemLookup: ItemLookup

  implicit val ec: ExecutionContext

  def lookupRequests(patronNumber: SierraPatronNumber): Route = {
    val userHolds =
      for {
        userHolds <- sierraService.getStacksUserHolds(patronNumber)

        holdsWithCatalogueIds <- Future.sequence(
          userHolds.right.get.holds.map { hold: StacksHold =>
            itemLookup
              .bySourceIdentifier(hold.sourceIdentifier)
              .map {
                case Left(elasticError: ElasticsearchError) =>
                  warn(
                    s"Unable to look up $hold in Elasticsearch: ${elasticError}"
                  )
                  hold

                case Right(item: Item[IdState.Identified]) =>
                  hold.copy(item = Some(item))
              }
          }
        )

        updatedHolds = userHolds.right.get.copy(holds = holdsWithCatalogueIds)
      } yield updatedHolds

    onComplete(userHolds) {
      case Success(value) => complete(DisplayResultsList(value))
      case Failure(err)   => failWith(err)
    }
  }
}
