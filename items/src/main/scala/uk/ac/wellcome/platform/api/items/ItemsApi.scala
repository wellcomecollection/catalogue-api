package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import uk.ac.wellcome.platform.api.common.models.display.DisplayStacksWork
import uk.ac.wellcome.platform.api.common.models.{
  StacksWork,
  StacksWorkIdentifier
}
import uk.ac.wellcome.platform.api.common.services.StacksService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait ItemsApi extends FailFastCirceSupport {

  import akka.http.scaladsl.server.Directives._
  import io.circe.generic.auto._

  implicit val ec: ExecutionContext
  implicit val stacksWorkService: StacksService

  val routes: Route = concat(
    pathPrefix("works") {
      path(Segment) { id: String =>
        get {
          val result: Future[StacksWork] =
            stacksWorkService.getStacksWork(
              StacksWorkIdentifier(id)
            )

          onComplete(result) {
            case Success(value) => complete(DisplayStacksWork(value))
            case Failure(err)   => failWith(err)
          }
        }
      }
    }
  )
}
