package weco.api.search.images

import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.request.SingleImageIncludes

class ImagesAggregationsTest
    extends ApiImagesTestBase
    with TestDocumentFixtures
    with CatalogueJsonUtil {
  it("aggregates by license") {
    val images = (0 to 6).map(i => s"images.different-licenses.$i")
    val displayImages = images
      .map(getDisplayImage)
      .sortBy(w => getKey(w, "id").get.asString)
      .map(_.withIncludes(SingleImageIncludes.none).noSpaces)

    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(imagesIndex, images: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/images?aggregations=locations.license"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = images.size)},
              "aggregations": {
                "type" : "Aggregations",
                "license": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : {
                        "id" : "cc-by",
                        "label" : "Attribution 4.0 International (CC BY 4.0)",
                        "type" : "License",
                        "url" : "http://creativecommons.org/licenses/by/4.0/"
                      },
                      "count" : 5,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "pdm",
                        "label" : "Public Domain Mark",
                        "type" : "License",
                        "url" : "https://creativecommons.org/share-your-work/public-domain/pdm/"
                      },
                      "count" : 2,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayImages.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by the canonical source's contributor agent labels") {
    val images = (0 to 2).map(i => s"images.contributors.$i")
    val displayImages = images
      .map(getDisplayImage)
      .sortBy(w => getKey(w, "id").get.asString)
      .map(_.withIncludes(SingleImageIncludes.none).noSpaces)

    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(imagesIndex, images: _*)

        assertJsonResponse(
          routes,
          path =
            s"$rootPath/images?aggregations=source.contributors.agent.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = images.size)},
              "aggregations": {
                "type" : "Aggregations",
                "source.contributors.agent.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data": {
                        "label" : "carrots",
                        "type" : "Agent"
                      },
                      "count": 3,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": {
                        "label" : "parrots",
                        "type" : "Meeting"
                      },
                      "count": 1,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": {
                        "label" : "parrots",
                        "type" : "Organisation"
                      },
                      "count": 1,
                      "type": "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayImages.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by the canonical source's genres") {
    val images = (0 to 2).map(i => s"images.genres.$i")
    val displayImages = images
      .map(getDisplayImage)
      .sortBy(w => getKey(w, "id").get.asString)
      .map(_.withIncludes(SingleImageIncludes.none).noSpaces)

    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(imagesIndex, images: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/images?aggregations=source.genres.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = images.size)},
              "aggregations": {
                "type" : "Aggregations",
                "source.genres.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data": {
                        "concepts" : [
                        ],
                        "label" : "Carrot counselling",
                        "type" : "Genre"
                      },
                      "count": 2,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": {
                        "concepts" : [
                        ],
                        "label" : "Emu entrepreneurship",
                        "type" : "Genre"
                      },
                      "count": 1,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": {
                        "concepts" : [
                        ],
                        "label" : "Falcon finances",
                        "type" : "Genre"
                      },
                      "count": 1,
                      "type": "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayImages.mkString(",")}
              ]
            }
          """
        }
    }
  }
}
