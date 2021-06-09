package weco.api.stacks.http.impl

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import weco.api.stacks.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class MemoryHttpClient(
  val baseUri: Uri,
  responses: Seq[(HttpRequest, HttpResponse)]
)(
  implicit val ec: ExecutionContext
) extends HttpClient {

  private val iterator = responses.toIterator

  override protected def makeRequest(request: HttpRequest): Future[HttpResponse] = Future {
    val (nextReq, nextResp) = iterator.next()

    if (nextReq != request) {
      throw new RuntimeException(s"Expected request $nextReq, but got $request")
    }

    nextResp
  }
}
