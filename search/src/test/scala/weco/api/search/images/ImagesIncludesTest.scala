package weco.api.search.images

import org.scalatest.funspec.AnyFunSpec

class ImagesIncludesTest extends AnyFunSpec with ApiImagesTestBase {
  describe("images includes") {
    it(
      "includes the source contributors on results from the list endpoint if we pass ?include=source.contributors"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.everything")

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?include=source.contributors"
          ) {
            Status.OK -> readResource(
              "expected_responses/include-list-contributors.json"
            )
          }
      }
    }

    it(
      "includes the source contributors on a result from the single image endpoint if we pass ?include=source.contributors"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.everything")

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.everything")}?include=source.contributors"
          ) {
            Status.OK -> readResource(
              "expected_responses/include-image-contributors.json"
            )
          }
      }
    }

    it(
      "includes the source languages on results from the list endpoint if we pass ?include=source.languages"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.everything")

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?include=source.languages"
          ) {
            Status.OK -> readResource(
              "expected_responses/include-list-languages.json"
            )
          }
      }
    }

    it("includes the source subjects in a list") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.subjects.sounds")
          assertJsonResponseContains(
            routes,
            path = s"$rootPath/images?include=source.subjects",
            locator = responseJson => {
              responseJson.hcursor
                .downField("results")
                .downArray
                .downField("source")
                .downField("subjects")
                .focus
                .get
            },
            expectedJson = """[
              {
                "label" : "Square sounds",
                "concepts" : [
                ],
                "type" : "Subject"
              }
            ]"""
          )
      }
    }

    it(
      "includes the source languages on a result from the single image endpoint if we pass ?include=source.languages"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.everything")

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.everything")}?include=source.languages"
          ) {
            Status.OK -> readResource(
              "expected_responses/include-image-languages.json"
            )
          }
      }
    }

    it(
      "includes the source genres on results from the list endpoint if we pass ?include=source.genres"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.everything")

          assertJsonResponse(routes, s"$rootPath/images?include=source.genres") {
            Status.OK -> readResource(
              "expected_responses/include-list-genres.json"
            )
          }
      }
    }

    it(
      "includes the source genres on a result from the single image endpoint if we pass ?include=source.genres"
    ) {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.everything")

          assertJsonResponse(
            routes,
            s"$rootPath/images/${getTestImageId("images.everything")}?include=source.genres"
          ) {
            Status.OK -> readResource(
              "expected_responses/include-image-genres.json"
            )
          }
      }
    }

    it("includes the source subjects on a single image") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, "images.subjects.sounds")

          assertJsonResponseContains(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.subjects.sounds")}?include=source.subjects",
            locator = responseJson =>
              responseJson.hcursor
                .downField("source")
                .downField("subjects")
                .focus
                .get,
            expectedJson = """
                [
                  {
                    "label" : "Square sounds",
                    "concepts" : [
                    ],
                    "type" : "Subject"
                  }
                ]
              """
          )
      }
    }
  }
}
