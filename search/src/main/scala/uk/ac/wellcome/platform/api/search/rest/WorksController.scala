package uk.ac.wellcome.platform.api.search.rest

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import weco.catalogue.display_model.models.Implicits._
import uk.ac.wellcome.api.display.models.DisplayWork
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.search.services.WorksService
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.models.ApiConfig
import weco.api.search.rest.SingleWorkDirectives
import weco.catalogue.display_model.models.{DisplayWork, WorksIncludes}
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.http.models.ContextResponse

import scala.concurrent.{ExecutionContext, Future}

class WorksController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  worksIndex: Index
)(implicit val ec: ExecutionContext)
    extends Tracing
    with SingleWorkDirectives {
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
              case Left(err) => elasticError("Work", err)
              case Right(resultList) =>
                extractPublicUri { requestUri =>
                  complete(
                    ContextResponse(
                      contextUrl = contextUrl,
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

          worksService
            .findById(id)(index)
            .mapVisible { work: Work.Visible[Indexed] =>
              Future.successful(workFound(work, includes))
            }
        }
      }
    }

  private def workFound(work: Work.Visible[Indexed],
                        includes: WorksIncludes): Route =
    complete(
      ContextResponse(
        contextUrl = contextUrl,
        result = DisplayWork(work, includes)
      )
    )

  private lazy val worksService = new WorksService(elasticsearchService)
}
