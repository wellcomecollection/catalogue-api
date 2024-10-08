package weco.api.search.rest

import org.apache.pekko.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import weco.Tracing
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.ApiConfig
import weco.api.search.models.request.WorksIncludes
import weco.api.search.services.WorksService

import scala.concurrent.{ExecutionContext, Future}

class WorksController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  worksIndex: Index
)(implicit val ec: ExecutionContext)
    extends Tracing
    with CatalogueJsonUtil
    with SingleWorkDirectives {

  def multipleWorks(params: MultipleWorksParams): Route =
    get {
      withFuture {
        transactFuture("GET /works") {
          val searchOptions = params.searchOptions(apiConfig)

          worksService
            .listOrSearch(worksIndex, searchOptions)
            .map {
              case Left(err) => elasticError(documentType = "Work", err)

              case Right(resultList) =>
                extractPublicUri { requestUri =>
                  complete(
                    DisplayResultList(
                      resultList = resultList,
                      searchOptions = searchOptions,
                      includes = params.include.getOrElse(WorksIncludes.none),
                      requestUri = requestUri
                    )
                  )
                }
            }
        }
      }
    }

  def singleWork(id: String, params: SingleWorkParams): Route =
    get {
      withFuture {
        transactFuture("GET /works/{workId}") {
          val includes = params.include.getOrElse(WorksIncludes.none)

          worksService
            .findById(id)(worksIndex)
            .mapVisible(work =>
              Future.successful(complete(work.display.withIncludes(includes))))
        }
      }
    }

  def countWorkTypes(
    index: Index
  ): Future[Either[ElasticsearchError, Map[String, Int]]] =
    worksService.countWorkTypes(index)

  private lazy val worksService = new WorksService(elasticsearchService)
}
