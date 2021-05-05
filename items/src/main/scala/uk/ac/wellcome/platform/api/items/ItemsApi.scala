package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.server.Route
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.common.models.display.DisplayStacksWork
import uk.ac.wellcome.platform.api.common.models.StacksWork
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

trait ItemsApi extends CustomDirectives with Tracing {

  implicit val ec: ExecutionContext
  implicit val stacksWorkService: StacksService

  val routes: Route = concat(
    pathPrefix("works") {
      path(Segment) { id: String =>
        Try { CanonicalId(id) } match {
          case Success(workId) =>
            get {
              withFuture {
                transactFuture("GET /works/{workId}/items") {
                  val result: Future[StacksWork] =
                    stacksWorkService.getStacksWork(workId)

                  result
                    .map(value => complete(DisplayStacksWork(value)))
                    .recoverWith {
                      case err => Future.successful(failWith(err))
                    }
                }
              }
            }

          case _ => notFound(s"Work not found for identifier $id")
        }
      }
    }
  )
}
