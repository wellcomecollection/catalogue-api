package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.server.Route
import uk.ac.wellcome.Tracing
import weco.api.stacks.items.responses.LookupItemStatus
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.util.{Success, Try}

trait ItemsApi extends LookupItemStatus with Tracing {
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
