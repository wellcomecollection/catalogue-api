package weco.api.search.rest

import akka.http.scaladsl.model.StatusCodes.Found
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import weco.api.search.elasticsearch.ElasticLookup
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

trait SingleWorkLookup extends CustomDirectives {
  implicit val ec: ExecutionContext
  val elasticLookup: ElasticLookup[Work[Indexed]]

  def lookupSingleWork(id: CanonicalId)(index: Index): Future[Either[Route, Work.Visible[Indexed]]] =
    elasticLookup
      .lookupById(id)(index)
      .map {
        case Right(Some(work: Work.Visible[Indexed])) =>
          Right(work)
        case Right(Some(work: Work.Redirected[Indexed])) =>
          Left(workRedirect(work))
        case Right(Some(_: Work.Invisible[Indexed])) =>
          Left(gone("This work has been deleted"))
        case Right(Some(_: Work.Deleted[Indexed])) =>
          Left(gone("This work has been deleted"))
        case Right(None) =>
          Left(notFound(s"Work not found for identifier $id"))
        case Left(err) => Left(elasticError(err))
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
