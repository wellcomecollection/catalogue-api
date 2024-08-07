package weco.api.search

import org.apache.pekko.http.scaladsl.model.ContentTypes
import io.circe.Json
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.works.ApiWorksTestBase

class ApiSearchTemplatesTest extends AnyFunSpec with ApiWorksTestBase with Matchers {
  it("renders a list of available search templates") {
    checkJson { json =>
      json.isObject shouldBe true
      val templates = getKey(json, "templates")
      templates should not be empty
      templates map { json =>
        getLength(json).get should be > 0
      }
    }
  }

  private def checkJson(f: Json => Unit): Unit =
    withApi { routes =>
      Get(s"$rootPath/search-templates.json") ~> routes ~> check {
        status shouldEqual Status.OK
        contentType shouldEqual ContentTypes.`application/json`
        f(parseJson(responseAs[String]))
      }
    }
}
