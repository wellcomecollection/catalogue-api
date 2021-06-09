package weco.api.stacks.http

import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.Uri.Path.Slash
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {
  val baseUri: Uri

  implicit val ec: ExecutionContext

  protected def makeRequest(request: HttpRequest): Future[HttpResponse]

  private def buildUri(
    path: Path,
    params: Map[String, String]
  ): Uri =
    baseUri
      .withPath(baseUri.path ++ Slash(path))
      .withQuery(Query(params))

  def get(path: Path, params: Map[String, String] = Map.empty): Future[HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = buildUri(path, params)
    )

    makeRequest(request)
  }

  def post[In](
    path: Path,
    body: Option[In] = None,
    params: Map[String, String] = Map.empty,
    headers: List[HttpHeader] = Nil)(
    implicit m: Marshaller[In, RequestEntity]
  ): Future[HttpResponse] =
    for {
      entity <- body match {
        case Some(body) => Marshal(body).to[RequestEntity]
        case None       => Future.successful(HttpEntity.Empty)
      }

      request = HttpRequest(
        HttpMethods.POST,
        uri = buildUri(path, params),
        headers = headers,
        entity = entity
      )

      response <- makeRequest(request)
    } yield response
}
