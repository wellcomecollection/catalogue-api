package weco.api.stacks.http.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import weco.api.stacks.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpClient(val baseUri: Uri)(implicit system: ActorSystem, val ec: ExecutionContext) extends HttpClient {
  override def singleRequest(request: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(request)
}
