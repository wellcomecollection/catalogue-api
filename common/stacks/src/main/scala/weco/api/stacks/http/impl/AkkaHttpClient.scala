package weco.api.stacks.http.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import weco.api.stacks.http.SierraAuthenticatedHttpClient
import weco.api.stacks.models.SierraAccessToken

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpClient(
  val baseUri: Uri,
  val credentials: BasicHttpCredentials
)(
  implicit
  val ec: ExecutionContext,
  val system: ActorSystem,
  val um: Unmarshaller[HttpResponse, SierraAccessToken]
) extends SierraAuthenticatedHttpClient {

  override protected def makeSingleRequest(request: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(request)

  override val tokenPath: Path = Path("v5/token")
}
