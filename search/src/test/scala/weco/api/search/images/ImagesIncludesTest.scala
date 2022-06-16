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

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?include=source.languages"
          ) {
            Status.OK ->
              """
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "id" : "xnb29lxt",
                      "locations" : [
                        {
                          "accessConditions" : [
                          ],
                          "license" : {
                            "id" : "cc-by",
                            "label" : "Attribution 4.0 International (CC BY 4.0)",
                            "type" : "License",
                            "url" : "http://creativecommons.org/licenses/by/4.0/"
                          },
                          "locationType" : {
                            "id" : "iiif-image",
                            "label" : "IIIF Image API",
                            "type" : "LocationType"
                          },
                          "type" : "DigitalLocation",
                          "url" : "https://iiif.wellcomecollection.org/image/Icx.jpg/info.json"
                        }
                      ],
                      "source" : {
                        "id" : "eealituc",
                        "languages" : [
                        ],
                        "title" : "title-CsSvOQ3XSc",
                        "type" : "Work"
                      },
                      "thumbnail" : {
                        "accessConditions" : [
                        ],
                        "license" : {
                          "id" : "cc-by",
                          "label" : "Attribution 4.0 International (CC BY 4.0)",
                          "type" : "License",
                          "url" : "http://creativecommons.org/licenses/by/4.0/"
                        },
                        "locationType" : {
                          "id" : "iiif-image",
                          "label" : "IIIF Image API",
                          "type" : "LocationType"
                        },
                        "type" : "DigitalLocation",
                        "url" : "https://iiif.wellcomecollection.org/image/Icx.jpg/info.json"
                      },
                      "type" : "Image"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 1,
                  "type" : "ResultList"
                }
                """
          }
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
            path =
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

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/images/${getTestImageId("images.subjects.sounds")}?include=source.subjects"
          ) {
            Status.OK ->
              """
                {
                  "id" : "xnb29lxt",
                  "thumbnail" : {
                    "locationType" : {
                      "id" : "iiif-image",
                      "label" : "IIIF Image API",
                      "type" : "LocationType"
                    },
                    "url" : "https://iiif.wellcomecollection.org/image/Icx.jpg/info.json",
                    "license" : {
                      "id" : "cc-by",
                      "label" : "Attribution 4.0 International (CC BY 4.0)",
                      "url" : "http://creativecommons.org/licenses/by/4.0/",
                      "type" : "License"
                    },
                    "accessConditions" : [
                    ],
                    "type" : "DigitalLocation"
                  },
                  "locations" : [
                    {
                      "locationType" : {
                        "id" : "iiif-image",
                        "label" : "IIIF Image API",
                        "type" : "LocationType"
                      },
                      "url" : "https://iiif.wellcomecollection.org/image/Icx.jpg/info.json",
                      "license" : {
                        "id" : "cc-by",
                        "label" : "Attribution 4.0 International (CC BY 4.0)",
                        "url" : "http://creativecommons.org/licenses/by/4.0/",
                        "type" : "License"
                      },
                      "accessConditions" : [
                      ],
                      "type" : "DigitalLocation"
                    }
                  ],
                  "source" : {
                    "id" : "eealituc",
                    "title" : "title-CsSvOQ3XSc",
                    "subjects" : [
                      {
                        "label" : "Square sounds",
                        "concepts" : [
                        ],
                        "type" : "Subject"
                      }
                    ],
                    "type" : "Work"
                  },
                  "type" : "Image"
                }
                """
          }
      }
    }
  }
}
