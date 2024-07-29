package weco.api.search.fixtures

import org.apache.pekko.http.scaladsl.model.{ContentTypes, StatusCode}
import org.apache.pekko.http.scaladsl.server.Route
import io.circe.Json
import org.scalatest.TestSuite
import weco.api.search.ApiTestBase

trait LocalJsonServerFixture extends ApiTestBase {
  this: TestSuite =>

  class WorksJsonServer(route: Route) extends JsonServer {
    def getJson(path: String): Json =
      eventually {
        Get(path) ~> route ~> check {
          contentType shouldEqual ContentTypes.`application/json`
          status shouldEqual Status.OK
          parseJson(responseAs[String])
        }
      }

    def failToGet(path: String): StatusCode = eventually {
      Get(path) ~> route ~> check {
        status shouldNot equal(Status.OK)
        status
      }
    }
  }
}
