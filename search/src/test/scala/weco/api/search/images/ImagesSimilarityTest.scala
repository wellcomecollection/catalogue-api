package weco.api.search.images

import weco.api.search.fixtures.TestDocumentFixtures

class ImagesSimilarityTest extends ApiImagesTestBase with TestDocumentFixtures {
  it("includes visually similar images with ?include=visuallySimilar") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestDocuments(
          imagesIndex,
          (0 to 5).map(i => s"images.similar-features-and-palettes.$i"): _*
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/images/fxlggzx3?include=visuallySimilar"
        ) {
          Status.OK -> readResource(
            "expected_responses/visually-similar-features-and-palettes.json"
          )
        }
    }
  }

  it("includes images with similar features with ?include=withSimilarFeatures") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestDocuments(
          imagesIndex,
          (0 to 5).map(i => s"images.similar-features.$i"): _*
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/images/1bxltcv6?include=withSimilarFeatures"
        ) {
          Status.OK -> readResource(
            "expected_responses/visually-similar-features.json"
          )
        }
    }
  }

  it(
    "includes images with similar color palettes with ?include=withSimilarColors"
  ) {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestDocuments(
          imagesIndex,
          (0 to 5).map(i => s"images.similar-palettes.$i"): _*
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/images/tsmrwj5f?include=withSimilarColors"
        ) {
          Status.OK -> readResource(
            "expected_responses/visually-similar-palettes.json"
          )
        }
    }
  }

  it("never includes visually similar images on an images search") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestDocuments(
          imagesIndex,
          (0 to 5).map(i => s"images.similar-features-and-palettes.$i"): _*
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/images?query=focaccia&include=visuallySimilar"
        ) {
          Status.BadRequest -> badRequest(
            description =
              "include: 'visuallySimilar' is not a valid value. Please choose one of: ['source.contributors', 'source.languages', 'source.genres']"
          )
        }
    }
  }
}
