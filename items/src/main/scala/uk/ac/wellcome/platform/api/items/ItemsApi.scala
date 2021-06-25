package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.Tracing
import weco.api.items.responses.LookupItemStatus
import weco.api.search.models.ApiConfig
import weco.api.stacks.services.{SierraService, WorkLookup}
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

class ItemsApi(
  val sierraService: SierraService,
  val workLookup: WorkLookup,
  val index: Index
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
