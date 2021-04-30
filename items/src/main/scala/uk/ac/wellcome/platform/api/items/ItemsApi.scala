package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.common.models.display.DisplayStacksWork
import uk.ac.wellcome.platform.api.common.models.StacksWork
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}

trait ItemsApi extends CustomDirectives with Tracing with FailFastCirceSupport {

  implicit val ec: ExecutionContext
  implicit val stacksWorkService: StacksService

  val routes: Route = concat(
    pathPrefix("works") {
      path(Segment) {
        id: String =>
          get {
            withFuture {
              transactFuture("GET /works/{imageId}/items") {
                val result: Future[StacksWork] =
                  stacksWorkService.getStacksWork(
                    CanonicalId(id)
                  )

                result
                  .map(value => complete(DisplayStacksWork(value)))
                  .recoverWith {
                    case err => Future.successful(failWith(err))
                  }
              }
            }
          }
      }
    }
  )
}
