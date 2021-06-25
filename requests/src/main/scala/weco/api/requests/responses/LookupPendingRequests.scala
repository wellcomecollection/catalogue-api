package weco.api.requests.responses

import akka.http.scaladsl.server.Route
import uk.ac.wellcome.platform.api.common.models.display.DisplayResultsList
import uk.ac.wellcome.platform.api.common.services.SierraService
import weco.api.search.rest.CustomDirectives
import weco.api.stacks.services.ItemLookup
import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber

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
          userHolds.right.get.holds.map { hold =>
            itemLookup
              .bySourceIdentifier(hold.sourceIdentifier)
              .map {
                case Left(elasticError) =>
                  warn(s"Unable to look up $hold in Elasticsearch")
                  hold

                case Right(item) =>
                  hold.copy(canonicalId = Some(item.id.canonicalId))
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
