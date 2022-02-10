package weco.api.search.images

import org.scalatest.prop.TableDrivenPropertyChecks
import weco.catalogue.config.ElasticConfig

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

    it("looking for a non-existent index") {
      val indexName = createIndexName

      val testPaths = Table(
        "path",
        s"$rootPath/images?_index=$indexName",
        s"$rootPath/images?_index=$indexName&query=fish",
        s"$rootPath/images/$createCanonicalId?_index=$indexName"
      )

      withApi { route =>
        forAll(testPaths) { path =>
          assertNotFound(route)(
            path,
            description = s"There is no index $indexName"
          )
        }
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
      s"$rootPath/images/$createCanonicalId"
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
}
