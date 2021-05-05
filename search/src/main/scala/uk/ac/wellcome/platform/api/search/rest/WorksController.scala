package uk.ac.wellcome.platform.api.search.rest

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.api.display.models.Implicits._
import uk.ac.wellcome.api.display.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.search.services.WorksService
import weco.api.search.elasticsearch.{
  ElasticsearchService,
  VisibleWorkDirectives
}
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.http.models.ContextResponse

import scala.concurrent.{ExecutionContext, Future}

class WorksController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  worksIndex: Index
)(implicit ec: ExecutionContext)
    extends Tracing
    with VisibleWorkDirectives {
  import ContextResponse.encoder
  import DisplayResultList.encoder

  def multipleWorks(params: MultipleWorksParams): Route =
    get {
      withFuture {
        transactFuture("GET /works") {
          val searchOptions = params.searchOptions(apiConfig)
          val index =
            params._index.map(Index(_)).getOrElse(worksIndex)
          worksService
            .listOrSearch(index, searchOptions)
            .map {
              case Left(err) => elasticError(err)
              case Right(resultList) =>
                extractPublicUri { requestUri =>
                  complete(
                    ContextResponse(
                      context = contextUri,
                      DisplayResultList(
                        resultList = resultList,
                        searchOptions = searchOptions,
                        includes = params.include.getOrElse(WorksIncludes.none),
                        requestUri = requestUri
                      )
                    )
                  )
                }
            }
        }
      }
    }

  def singleWork(id: CanonicalId, params: SingleWorkParams): Route =
    get {
      withFuture {
        transactFuture("GET /works/{workId}") {
          val index =
            params._index.map(Index(_)).getOrElse(worksIndex)
          val includes = params.include.getOrElse(WorksIncludes.none)

          worksService.findById(id)(index).map { work =>
            visibleWork(id, work) { work: Work.Visible[Indexed] =>
              Future.successful(
                complete(
                  ContextResponse(
                    context = contextUri,
                    result = DisplayWork(work, includes)
                  )
                )
              )
            }
          }
        }
      }
    }

  private lazy val worksService = new WorksService(elasticsearchService)

  override def context: String = contextUri.toString
}
