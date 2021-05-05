package uk.ac.wellcome.platform.api.rest

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.platform.api.models.ApiConfig

class CustomDirectivesTest
    extends AnyFunSpec
    with Matchers
    with CustomDirectives
    with ScalatestRouteTest {

  implicit val apiConfig = ApiConfig(
    publicRootUri = Uri("https://api-test.wellcomecollection.org/catalogue/v2"),
    defaultPageSize = 10,
    contextSuffix = "context.json"
  )
  val context: String = contextUri

  describe("extractPublicUri") {
    val testRoute = get {
      path("test") {
        extractPublicUri { uri =>
          complete(uri.toString())
        }
      }
      pathPrefix("catalogue") {
        pathPrefix("v2") {
          path("test") {
            extractPublicUri { uri =>
              complete(uri.toString())
            }
          }
        }
      }
    }

    it("should return a URI with the configured public host") {
      Get("/test") ~> testRoute ~> check {
        responseAs[String] shouldBe "https://api-test.wellcomecollection.org/catalogue/v2/test"
      }
    }

    it(
      "should return a URI with the configured public root path, if the request URI doesn't already contain it"
    ) {
      Get("/catalogue/v2/test") ~> testRoute ~> check {
        responseAs[String] shouldBe "https://api-test.wellcomecollection.org/catalogue/v2/test"
      }
    }
  }
}
