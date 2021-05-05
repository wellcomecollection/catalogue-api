package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.common.models.display.DisplayStacksWork
import uk.ac.wellcome.platform.api.common.services.StacksService
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.stacks.services.WorksLookup
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

trait ItemsApi extends CustomDirectives with Tracing {

  implicit val ec: ExecutionContext
  implicit val stacksWorkService: StacksService
  val elasticsearchService: ElasticsearchService
  val index: Index

  private val worksLookup = new WorksLookup(elasticsearchService)

  private def getStacksWork(id: CanonicalId): Future[Route] =
    for {
      work <- worksLookup.byWorkId(id)(index)
      _ = println(s"Got work $work")

      result <- stacksWorkService.getStacksWork(id)

      route = complete(DisplayStacksWork(result))
    } yield route

  val routes: Route = concat(
    pathPrefix("works") {
      path(Segment) { id: String =>
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
