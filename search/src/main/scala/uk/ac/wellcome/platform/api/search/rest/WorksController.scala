package uk.ac.wellcome.platform.api.search.rest

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.model.StatusCodes.Found
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.api.display.models.Implicits._
import uk.ac.wellcome.api.display.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import uk.ac.wellcome.platform.api.search.services.WorksService
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.http.models.ContextResponse

class WorksController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  worksIndex: Index
)(implicit ec: ExecutionContext)
    extends Tracing
    with CustomDirectives {
  import DisplayResultList.encoder
  import ContextResponse.encoder

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
          worksService
            .findById(id)(index)
            .map {
              case Right(work: Work.Visible[Indexed]) =>
                workFound(work, includes)
              case Right(work: Work.Redirected[Indexed]) =>
                workRedirect(work)
              case Right(_: Work.Invisible[Indexed]) =>
                gone("This work has been deleted")
              case Right(_: Work.Deleted[Indexed]) =>
                gone("This work has been deleted")
              case Left(err) => elasticError("Work", err)
            }
        }
      }
    }

  def workRedirect(work: Work.Redirected[Indexed]): Route =
    extractPublicUri { uri =>
      val newPath =
        (work.redirectTarget.canonicalId.underlying :: uri.path.reverse.tail).reverse

      // We use a relative URL here so that redirects keep you on the same host.
      //
      // e.g. on https://api-stage.wc.org, we tell the API that it's running at
      // https://api.wc.org so URLs in responses look the same, but we don't want
      // the stage API to send redirects to the prod API.
      //
      // Relative URLs are explicitly supported in HTTP 302 Redirects, see
      // https://greenbytes.de/tech/webdav/draft-ietf-httpbis-p2-semantics-17.html#rfc.section.9.5
      // https://stackoverflow.com/q/8250259/1558022
      redirect(uri.withPath(newPath).toRelative, Found)
    }

  def workFound(work: Work.Visible[Indexed], includes: WorksIncludes): Route =
    complete(
      ContextResponse(
        context = contextUri,
        result = DisplayWork(work, includes)
      )
    )

  private lazy val worksService = new WorksService(elasticsearchService)

  override def context: String = contextUri.toString
}
