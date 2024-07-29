package weco.api.items

import org.apache.pekko.http.scaladsl.server.Route
import weco.Tracing
import weco.api.items.responses.LookupItemStatus
import weco.api.items.services.{ItemUpdateService, WorkLookup}
import weco.api.search.models.ApiConfig
import weco.catalogue.display_model.rest.IdentifierDirectives
import weco.http.FutureDirectives

import scala.concurrent.ExecutionContext

class ItemsApi(
  val itemUpdateService: ItemUpdateService,
  val workLookup: WorkLookup
)(
  implicit
  val ec: ExecutionContext,
  val apiConfig: ApiConfig
) extends LookupItemStatus
    with IdentifierDirectives
    with FutureDirectives
    with Tracing {
  val routes: Route = concat(
    pathPrefix("works") {
      path(Segment) {
        case id if looksLikeCanonicalId(id) =>
          get {
            withFuture {
              transactFuture("GET /works/{workId}/items") {
                lookupStatus(id)
              }
            }
          }

        case id =>
          notFound(s"Work not found for identifier $id")
      }
    },
    pathPrefix("management") {
      concat(path("healthcheck") {
        get {
          complete("message" -> "ok")
        }
      })
    }
  )
}
