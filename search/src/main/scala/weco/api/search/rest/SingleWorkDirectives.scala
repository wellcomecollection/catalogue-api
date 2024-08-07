package weco.api.search.rest

import org.apache.pekko.http.scaladsl.model.StatusCodes.Found
import org.apache.pekko.http.scaladsl.server.Route
import weco.api.search.elasticsearch.ElasticsearchError
import weco.api.search.models.index.IndexedWork

import scala.concurrent.{ExecutionContext, Future}

trait SingleWorkDirectives extends CustomDirectives {
  implicit val ec: ExecutionContext

  implicit class RouteOps(
    work: Future[Either[ElasticsearchError, IndexedWork]]
  ) {
    def mapVisible(
      f: IndexedWork.Visible => Future[Route]
    ): Future[Route] =
      work.map {
        case Right(work: IndexedWork.Visible) =>
          withFuture(f(work))
        case Right(work: IndexedWork.Redirected) =>
          workRedirect(work)
        case Right(_: IndexedWork.Invisible) | Right(_: IndexedWork.Deleted) =>
          gone("This work has been deleted")
        case Left(err) =>
          elasticError(documentType = "Work", err)
      }
  }

  private def workRedirect(work: IndexedWork.Redirected): Route =
    extractPublicUri { uri =>
      val newPath =
        (work.redirectTarget.canonicalId :: uri.path.reverse.tail).reverse

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
