package weco.api.search.images

class ImagesSimilarityTest extends ApiImagesTestBase {

  /**
    * These tests treat the simple presence of the requested property as a proxy
    * for populating the similar image lists.  Essentially, this is showing the
    * connection between the `include` querystring parameter and the resulting JSON.
    *
    * The thresholds are set too high to be able to actually return similar images in
    * these dummy indices, so the content of these image lists is irrelevant to the tests.
    *
    * Tests that show that the similar images functionality behaves as expected
    * can be found in ../services/ImagesServiceTest.scala
    * and there should also be rank tests to cover whether the images returned are appropriately similar
    */
  describe("image similarity") {

    it("includes visually similar images with ?include=visuallySimilar") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            "images.similar-features-and-palettes.0"
          )

          assertJsonResponseLike(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.similar-features-and-palettes.0")}?include=visuallySimilar",
            assertion = responseJson => {
              assert(responseJson.asObject.get.contains("visuallySimilar"))
              assert(!responseJson.asObject.get.contains("withSimilarFeatures"))
              assert(!responseJson.asObject.get.contains("withSimilarColors"))
            }
          )
      }
    }

    it(
      "includes images with similar features with ?include=withSimilarFeatures"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            "images.similar-features.0"
          )

          assertJsonResponseLike(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.similar-features.0")}?include=withSimilarFeatures",
            assertion = responseJson => {
              assert(responseJson.asObject.get.contains("withSimilarFeatures"))
              assert(!responseJson.asObject.get.contains("visuallySimilar"))
              assert(!responseJson.asObject.get.contains("withSimilarColors"))
            }
          )
      }
    }

    it(
      "includes images with similar color palettes with ?include=withSimilarColors"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            "images.similar-palettes.0"
          )
          assertJsonResponseLike(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.similar-palettes.0")}?include=withSimilarColors",
            assertion = responseJson => {
              assert(responseJson.asObject.get.contains("withSimilarColors"))
              assert(!responseJson.asObject.get.contains("visuallySimilar"))
              assert(!responseJson.asObject.get.contains("withSimilarFeatures"))
            }
          )
      }
    }

    it("includes all requested similarity fields in a comma-separated list") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            "images.similar-features-and-palettes.0"
          )

          assertJsonResponseLike(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.similar-features-and-palettes.0")}?include=visuallySimilar,withSimilarFeatures,withSimilarColors",
            assertion = responseJson => {
              assert(responseJson.asObject.get.contains("visuallySimilar"))
              assert(responseJson.asObject.get.contains("withSimilarFeatures"))
              assert(responseJson.asObject.get.contains("withSimilarColors"))
            }
          )
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
                "include: 'visuallySimilar' is not a valid value. Please choose one of: ['source.contributors', 'source.languages', 'source.genres', 'source.subjects']"
            )
          }
      }
    }
  }
}
