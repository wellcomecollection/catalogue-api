package weco.api.search

import akka.http.scaladsl.model.StatusCodes._
import weco.api.search.works.ApiWorksTestBase

import scala.io.Source

class ApiContextTest extends ApiWorksTestBase {

  it("returns a context for v2") {
    withApi { routes =>
      val path = s"$rootPath/context.json"
      assertJsonResponse(routes, path)(
        OK ->
          Source.fromResource("context-v2.json").getLines.mkString("\n")
      )
    }
  }
}
