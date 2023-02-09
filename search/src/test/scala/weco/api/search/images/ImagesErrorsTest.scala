package weco.api.search.images

import org.scalatest.prop.TableDrivenPropertyChecks
import weco.api.search.models.ElasticConfig

class ImagesErrorsTest
    extends ApiImagesTestBase
    with TableDrivenPropertyChecks {
  describe("returns a 404 for missing resources") {
    it("looking up an image that doesn't exist") {
      val id = "blahblah"

      withImagesApi {
        case (_, route) =>
          assertNotFound(route)(
            path = s"$rootPath/images/$id",
            description = s"Image not found for identifier $id"
          )
      }
    }

    it("looking up an image with a malformed identifier") {
      val badId = "zd224ncv]"

      withApi { route =>
        assertNotFound(route)(
          path = s"$rootPath/images/$badId",
          description = s"Image not found for identifier $badId"
        )
      }
    }
  }

  describe("returns a 400 Bad Request for invalid parameters") {
    it("rejects an invalid color parameter") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/images?color=%3C",
          description =
            s"color: '<' is not a valid value. Please supply a hex string."
        )
      }
    }

    it("rejects multiple invalid color parameters") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/images?color=%3C,ff0000,<script>",
          description =
            s"color: '<', '<script>' are not valid values. Please supply hex strings."
        )
      }
    }
  }

  it("returns a 500 error if the default index doesn't exist") {
    val elasticConfig = ElasticConfig(
      worksIndex = createIndex,
      imagesIndex = createIndex
    )

    val testPaths = Table(
      "path",
      s"$rootPath/images",
      s"$rootPath/images?query=fish",
      s"$rootPath/images/aj0amjkh"
    )

    withRouter(elasticConfig) { routes =>
      forAll(testPaths) { path =>
        assertJsonResponse(routes, path)(
          Status.InternalServerError ->
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 500,
               |  "label": "Internal Server Error"
               |}
            """.stripMargin
        )
      }
    }
  }

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
