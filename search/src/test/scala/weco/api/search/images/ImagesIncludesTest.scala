package weco.api.search.images

class ImagesIncludesTest extends ApiImagesTestBase {
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
          assertJsonResponseContains(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.everything")}?include=source.genres",
            locator = responseJson => {
              responseJson.hcursor
                .downField("source")
                .downField("genres")
                .focus
                .get
            },
            expectedJson = """
               [
              {
                "concepts" : [
                  {
                    "label" : "U0oWO3ioLqbrury",
                    "type" : "Concept"
                  },
                  {
                    "label" : "7o2QTEqcIF9bQro",
                    "type" : "Concept"
                  },
                  {
                    "label" : "ssWozG8sqfYdoIO",
                    "type" : "Concept"
                  }
                ],
                "label" : "Crumbly cabbages",
                "type" : "Genre"
              },
              {
                "concepts" : [
                  {
                    "label" : "JIUj2LP1mxi643r",
                    "type" : "Concept"
                  },
                  {
                    "label" : "2da07kIJPZsamHd",
                    "type" : "Concept"
                  },
                  {
                    "label" : "A4kdSkosRRTtjUN",
                    "type" : "Concept"
                  }
                ],
                "label" : "Deadly durians",
                "type" : "Genre"
              }
            ]
             """
          )
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
