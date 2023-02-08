package weco.api.search.services

import weco.api.search.images.ApiImagesTestBase

class ImagesErrorsTest extends ApiImagesTestBase {
  describe("trying to get more images than ES allows") {
    val description = "Only the first 10000 images are available in the API. " +
      "If you want more images, you can download a snapshot of the complete catalogue: " +
      "https://developers.wellcomecollection.org/docs/datasets"

    it("a very large page") {
      withImagesApi {
        case (_, route) =>
          assertBadRequest(route)(
            path = s"$rootPath/images?page=10000",
            description = description
          )
      }
    }

    // This is a regression test for https://github.com/wellcometrust/platform/issues/3233
    //
    // We saw real requests like this, which we traced to an overflow in the
    // page offset we were requesting in Elasticsearch.
    it("so many pages that a naive (page * pageSize) would overflow") {
      withImagesApi {
        case (_, route) =>
          assertBadRequest(route)(
            path = s"$rootPath/images?page=2000000000&pageSize=100",
            description = description
          )
      }
    }

    it("the 101th page with 100 results per page") {
      withImagesApi {
        case (_, route) =>
          assertBadRequest(route)(
            path = s"$rootPath/images?page=101&pageSize=100",
            description = description
          )
      }
    }
  }
}
