package uk.ac.wellcome.platform.api.search.images

import org.scalatest.Assertion

class ImagesErrorsTest extends ApiImagesTestBase {
  describe("returns a 404 for missing resources") {
    it("looking up an image that doesn't exist") {
      val id = "blahblah"
      assertIsNotFound(
        s"$rootPath/images/$id",
        description = s"Image not found for identifier $id"
      )
    }
  }

  def assertIsNotFound(path: String, description: String): Assertion =
    withImagesApi {
      case (_, routes) =>
        assertJsonResponse(routes, path)(
          Status.NotFound ->
            notFound(
              description = description
            )
        )
    }
}
