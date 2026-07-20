package weco.api.search.openapi

import io.circe.Json
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.ApiTestBase

/** Checks that every endpoint the spec documents for this service exists, and that the
  * endpoints we keep out of the spec stay out of it.
  *
  * A route in Pekko is an opaque function, so we cannot enumerate what the API serves
  * and compare that against the spec. We can ask the router instead. An unmatched path
  * is the only thing that answers "Page not found for URL ..." (see SearchApi's
  * rejection handler), so any other response means the route exists. That catches a
  * spec documenting an endpoint this service does not serve.
  *
  * It does not catch the reverse. A new public path added to SearchApi and never
  * written down would pass, though the internal paths listed below are checked. The
  * concepts service uses express, which can enumerate its own routes, so the
  * equivalent test there (concepts/test/openapi.test.ts) compares both directions.
  */
class OpenApiSpecEndpointTest
    extends AnyFunSpec
    with Matchers
    with ApiTestBase {

  private val spec: Json = OpenApiSpec.parsed

  /** Endpoints this service serves on purpose but does not document: internal tooling
    * and the diff tool. If you document one of these, delete it from here.
    */
  private val undocumentedInternalPaths = Seq(
    "/search-templates.json",
    "/_elasticConfig",
    "/management/healthcheck",
    "/management/clusterhealth",
    "/management/_workTypes"
  )

  private def specPaths: Seq[String] =
    spec.hcursor
      .downField("paths")
      .keys
      .getOrElse(fail("The spec has no paths"))
      .toSeq

  /** The tag on an operation tells us which service serves it. */
  private def tagsFor(specPath: String): Set[String] =
    spec.hcursor
      .downField("paths")
      .downField(specPath)
      .downField("get")
      .downField("tags")
      .focus
      .flatMap(_.asArray)
      .getOrElse(fail(s"$specPath has no tags"))
      .flatMap(_.asString)
      .toSet

  /** `{id}` stands in for a canonical id, which must match `^[a-z0-9]+$`. */
  private def asRequestPath(specPath: String): String =
    specPath.replace("{id}", "b2ppvvzr")

  /** SearchApi answers "Page not found for URL ..." only when nothing matched the
    * path. Every other response means the route is there, including a 404 for a
    * document that doesn't exist.
    */
  private def isRouted(requestPath: String): Boolean =
    withApi { routes =>
      Get(requestPath) ~> routes ~> check {
        val description = io.circe.parser
          .parse(responseAs[String])
          .toOption
          .flatMap(_.hcursor.get[String]("description").toOption)
          .getOrElse("")

        !description.startsWith("Page not found for URL")
      }
    }

  describe("the endpoints this service serves") {
    it("serves every works and images endpoint the spec documents") {
      val documented = specPaths.filter { p =>
        val tags = tagsFor(p)
        tags.contains("Works") || tags.contains("Images")
      }

      documented should not be empty

      documented.foreach { specPath =>
        withClue(
          s"The spec documents $specPath, but SearchApi does not serve it: "
        ) {
          isRouted(asRequestPath(specPath)) shouldBe true
        }
      }
    }

    it(
      "does not serve the concepts endpoints, which the concepts service serves") {
      val conceptPaths = specPaths.filter(tagsFor(_).contains("Concepts"))

      conceptPaths should not be empty

      conceptPaths.foreach { specPath =>
        withClue(s"SearchApi unexpectedly serves $specPath: ") {
          isRouted(asRequestPath(specPath)) shouldBe false
        }
      }
    }

    it("recognises a path that does not exist") {
      // A negative control: without this, the assertions above would pass even if
      // isRouted always returned true.
      isRouted("/works-that-do-not-exist") shouldBe false
    }
  }

  describe("the endpoints this service keeps out of the spec") {
    it("serves the internal endpoints") {
      undocumentedInternalPaths.foreach { path =>
        withClue(s"$path is listed as internal, but is not served: ") {
          isRouted(path) shouldBe true
        }
      }
    }

    it("does not document the internal endpoints") {
      undocumentedInternalPaths.foreach { path =>
        withClue(s"$path is internal, but the spec documents it: ") {
          specPaths should not contain path
        }
      }
    }
  }
}
