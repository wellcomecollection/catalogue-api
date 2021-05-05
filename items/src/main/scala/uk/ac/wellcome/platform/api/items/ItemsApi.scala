package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.common.models.display.DisplayStacksWork
import uk.ac.wellcome.platform.api.common.services.StacksService
import weco.api.search.elasticsearch.{
  ElasticsearchService,
  VisibleWorkDirectives
}
import weco.api.stacks.services.WorkLookup
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

trait ItemsApi extends VisibleWorkDirectives with Tracing {

  implicit val ec: ExecutionContext
  implicit val stacksService: StacksService
  val elasticsearchService: ElasticsearchService
  val index: Index

  private val worksLookup = new WorkLookup(elasticsearchService)

  private def getStacksWork(id: CanonicalId): Future[Route] =
    worksLookup
      .byId(id)(index)
      .map { work =>
        visibleWork(id, work) { w: Work.Visible[Indexed] =>
          stacksService
            .getStacksWork(w)
            .map { stacksWork =>
              complete(DisplayStacksWork(stacksWork))
            }
        }
      }

  val routes: Route = concat(
    pathPrefix("works") {
      path(Segment) {
        id: String =>
          Try { CanonicalId(id) } match {
            case Success(workId) =>
              get {
                withFuture {
                  transactFuture("GET /works/{workId}/items") {
                    getStacksWork(workId)
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
