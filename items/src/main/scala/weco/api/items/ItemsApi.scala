package weco.api.items

import akka.http.scaladsl.server.Route
import weco.Tracing
import weco.api.items.responses.LookupItemStatus
import weco.api.items.services.{ItemUpdateService, WorkLookup}
import weco.api.search.models.ApiConfig
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

class ItemsApi(
  val itemUpdateService: ItemUpdateService,
  val workLookup: WorkLookup
)(
  implicit
  val ec: ExecutionContext,
  val apiConfig: ApiConfig
) extends LookupItemStatus
    with Tracing {
  val routes: Route = concat(
    pathPrefix("works") {
      path(Segment) { id: String =>
        Try { CanonicalId(id) } match {
          case Success(workId) =>
            get {
              withFuture {
                transactFuture("GET /works/{workId}/items") {
                  lookupStatus(workId)
                }
              }
            }

          case _ => notFound(s"Work not found for identifier $id")
        }
      }
    }
  )
}
