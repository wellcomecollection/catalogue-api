package weco.api.search.rest

import akka.http.scaladsl.model.StatusCodes.Found
import akka.http.scaladsl.server.Route
import weco.api.search.elasticsearch.ElasticsearchError
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.catalogue.internal_model.work.{Work, WorkState}

import scala.concurrent.{ExecutionContext, Future}

trait SingleWorkDirectives extends CustomDirectives {
  implicit val ec: ExecutionContext

  implicit class RouteOps(
    work: Future[Either[ElasticsearchError, Work[WorkState.Indexed]]]
  ) {
    def mapVisible(
      f: Work.Visible[WorkState.Indexed] => Future[Route],
      usingDefaultIndex: Boolean
    ): Future[Route] =
      work.map {
        case Right(work: Work.Visible[Indexed]) =>
          withFuture(f(work))
        case Right(work: Work.Redirected[Indexed]) =>
          workRedirect(work)
        case Right(_: Work.Invisible[Indexed]) =>
          gone("This work has been deleted")
        case Right(_: Work.Deleted[Indexed]) =>
          gone("This work has been deleted")
        case Left(err) =>
          elasticError(
            documentType = "Work",
            err = err,
            usingDefaultIndex = usingDefaultIndex
          )
      }
  }

  private def workRedirect(work: Work.Redirected[Indexed]): Route =
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
}
