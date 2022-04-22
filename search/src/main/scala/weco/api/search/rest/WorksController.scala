package weco.api.search.rest

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import weco.Tracing
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.api.search.models.ApiConfig
import weco.api.search.services.WorksService
import weco.catalogue.display_model.models.Implicits._
import weco.catalogue.display_model.models.{DisplayWork, WorksIncludes}
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

class WorksController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  worksIndex: Index
)(implicit val ec: ExecutionContext)
    extends Tracing
    with SingleWorkDirectives {

  def multipleWorks(params: MultipleWorksParams): Route =
    get {
      withFuture {
        transactFuture("GET /works") {
          val searchOptions = params.searchOptions(apiConfig)

          val userSpecifiedIndex = params._index.map(Index(_))
          val index = userSpecifiedIndex.getOrElse(worksIndex)

          worksService
            .listOrSearch(index, searchOptions)
            .map {
              case Left(err) =>
                elasticError(
                  documentType = "Work",
                  err = err,
                  usingUserSpecifiedIndex = userSpecifiedIndex.isDefined
                )

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

  def singleWork(id: CanonicalId, params: SingleWorkParams): Route =
    get {
      withFuture {
        transactFuture("GET /works/{workId}") {
          val userSpecifiedIndex = params._index.map(Index(_))
          val index = userSpecifiedIndex.getOrElse(worksIndex)

          val includes = params.include.getOrElse(WorksIncludes.none)

          worksService
            .findById(id)(index)
            .mapVisible(
              (work: Work.Visible[Indexed]) =>
                Future.successful(
                  complete(DisplayWork(work, includes))
                ),
              usingUserSpecifiedIndex = userSpecifiedIndex.isDefined
            )
        }
      }
    }

  def countWorkTypes(
    index: Index
  ): Future[Either[ElasticsearchError, Map[String, Int]]] =
    worksService.countWorkTypes(index)

  private lazy val worksService = new WorksService(elasticsearchService)
}
