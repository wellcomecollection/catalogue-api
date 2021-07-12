package weco.api.search.rest

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.models.ApiConfig

class CustomDirectivesTest
    extends AnyFunSpec
    with Matchers
    with CustomDirectives
    with ScalatestRouteTest {

  implicit val apiConfig = ApiConfig(
    publicRootUri = Uri("https://api-test.wellcomecollection.org/catalogue/v2"),
    defaultPageSize = 10
  )

  describe("extractPublicUri") {
    val testRoute = concat(
      path("test") {
        extractPublicUri { uri =>
          complete(uri.toString())
        }
      }
    )

    it("returns a URI with the configured public host and path") {
      Get("/test") ~> testRoute ~> check {
        responseAs[String] shouldBe "https://api-test.wellcomecollection.org/catalogue/v2/test"
      }
    }
  }
}
