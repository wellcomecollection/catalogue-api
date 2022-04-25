package weco.api.items.responses

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Route
import weco.api.items.models.DisplayItemsList
import weco.api.items.services._
import weco.api.search.rest.SingleWorkDirectives
import weco.api.stacks.models.CatalogueWork
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.Future

trait LookupItemStatus extends SingleWorkDirectives {
  import weco.catalogue.display_model.Implicits._

  val itemUpdateService: ItemUpdateService

  val workLookup: WorkLookup

  def lookupStatus(workId: CanonicalId): Future[Route] =
    workLookup.byCanonicalId(workId).flatMap {
      case Right(work: CatalogueWork) =>
        itemUpdateService
          .updateItems(work)
          .map { items =>
            complete(
              HttpResponse(
                entity = HttpEntity(
                  contentType = ContentTypes.`application/json`,
                  toJson(
                    DisplayItemsList(
                      totalResults = items.length,
                      results = items
                    )
                  )
                )
              )
            )
          }

      case Left(WorkNotFoundError(id)) =>
        Future(notFound(s"Work not found for identifier $id"))

      case Left(WorkGoneError(id)) =>
        Future(gone("This work has been deleted"))

      case Left(UnknownWorkError(id, err)) =>
        Future(internalError(err))
    }
}
