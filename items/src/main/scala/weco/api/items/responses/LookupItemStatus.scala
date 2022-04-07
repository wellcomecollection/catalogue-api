package weco.api.items.responses

import akka.http.scaladsl.server.Route
import weco.api.items.models.DisplayItemsList
import weco.api.items.services.{
  ItemUpdateService,
  UnknownWorkError,
  WorkLookup,
  WorkNotFoundError
}
import weco.api.search.rest.SingleWorkDirectives
import weco.catalogue.display_model.models.DisplayWork
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.Future

trait LookupItemStatus extends SingleWorkDirectives {
  val itemUpdateService: ItemUpdateService

  val workLookup: WorkLookup

  def lookupStatus(workId: CanonicalId): Future[Route] =
    workLookup.byCanonicalId(workId).flatMap {
      case Right(work: DisplayWork) =>
        itemUpdateService
          .updateItems(work)
          .map { items =>
            complete(DisplayItemsList(
              totalResults = items.length,
              results = items
            ))
          }

      case Left(WorkNotFoundError(id)) =>
        Future(notFound(s"There is no work with id $id"))

      case Left(UnknownWorkError(id, err)) =>
        Future(internalError(err))
    }
}
