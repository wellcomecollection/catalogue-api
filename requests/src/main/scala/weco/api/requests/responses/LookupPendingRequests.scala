package weco.api.requests.responses

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.platform.api.common.models.StacksUserIdentifier
import uk.ac.wellcome.platform.api.common.models.display.DisplayResultsList
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.api.stacks.services.ItemLookup

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LookupPendingRequests extends CustomDirectives {
  val sierraService: SierraService
  val itemLookup: ItemLookup
  val index: Index

  implicit val ec: ExecutionContext

  def lookupRequests(userIdentifier: StacksUserIdentifier): Route = {
    val userHolds =
      for {
        userHolds <- sierraService.getStacksUserHolds(userIdentifier)

        holdsWithCatalogueIds <-
          Future.sequence(
            userHolds.holds.map { hold =>
              itemLookup.bySourceIdentifier(hold.sourceIdentifier)(index)
                .map {
                  case Left(elasticError) =>
                    warn(s"Unable to look up $hold in Elasticsearch")
                    hold

                  case Right(canonicalId) =>
                    hold.copy(canonicalId = Some(canonicalId))
                }
            }
          )

        updatedHolds = userHolds.copy(holds = holdsWithCatalogueIds)
      } yield updatedHolds

    onComplete(userHolds) {
      case Success(value) => complete(DisplayResultsList(value))
      case Failure(err)   => failWith(err)
    }
  }
}
