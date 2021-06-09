package weco.api.stacks.http

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait HttpClient {
  def makeRequest(request: HttpRequest): Future[HttpResponse]
}
