package weco.api.search.management

import org.apache.pekko.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ManifestRouteTest
    extends AnyFunSpec
    with Matchers
    with ScalatestRouteTest {

  describe("the manifest endpoint") {
    it("reports the commit the image was built from") {
      Get("/manifest") ~> ManifestRoute.routeFor(
        commit = "3e82edf9",
        startedAt = "2026-09-11T08:00:00Z"
      ) ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        responseAs[String] shouldBe
          """{"commit":"3e82edf9","startedAt":"2026-09-11T08:00:00Z"}"""
      }
    }

    it("is never cached, so the tracker sees the running commit") {
      Get("/manifest") ~> ManifestRoute.route ~> check {
        header("Cache-Control").map(_.value()) shouldBe Some("no-store")
      }
    }

    it("drops characters that would break the JSON") {
      Get("/manifest") ~> ManifestRoute.routeFor(
        commit = """ab"c,1 2""",
        startedAt = "2026-09-11T08:00:00Z"
      ) ~> check {
        responseAs[String] should include(""""commit":"abc12"""")
      }
    }

  }

  describe("reading the commit from the environment") {
    it("uses BUILD_COMMIT when the image was built with it") {
      ManifestRoute.commitFrom(Map("BUILD_COMMIT" -> "3e82edf9")) shouldBe "3e82edf9"
    }

    it("says unknown when the build argument was not passed") {
      ManifestRoute.commitFrom(Map("BUILD_COMMIT" -> "")) shouldBe "unknown"
    }

    it("says unknown when the variable is absent") {
      ManifestRoute.commitFrom(Map.empty) shouldBe "unknown"
    }
  }
}
