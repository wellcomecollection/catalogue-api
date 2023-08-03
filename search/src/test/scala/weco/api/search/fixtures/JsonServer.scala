package weco.api.search.fixtures

import akka.http.scaladsl.model.StatusCode
import io.circe.Json

trait JsonServer {
  def getJson(path: String): Json
  def failToGet(path: String): StatusCode
}
