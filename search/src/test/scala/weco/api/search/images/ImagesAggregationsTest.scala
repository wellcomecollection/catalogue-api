package weco.api.search.images

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.models.request.SingleImageIncludes

class ImagesAggregationsTest extends AnyFunSpec
with ApiImagesTestBase {
  it("aggregates by license") {
    val images = (0 to 6).map(i => s"images.different-licenses.$i")
    val displayImages = images
      .map(getDisplayImage)
      .sortBy(w => getKey(w, "id").get.asString)
      .map(_.withIncludes(SingleImageIncludes.none).noSpaces)

    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestDocuments(imagesIndex, images: _*)

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
                        "label" : "Attribution 4.0 International (CC BY 4.0)"
                      },
                      "count" : 5,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "pdm",
                        "label" : "Public Domain Mark"
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
        indexTestDocuments(imagesIndex, images: _*)

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
                        "id" : "carrots",
                        "label" : "carrots"
                      },
                      "count": 3,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": {
                        "id" : "parrots",
                        "label" : "parrots"
                      },
                      "count": 2,
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
        indexTestDocuments(imagesIndex, images: _*)

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
                        "id" : "Carrot counselling",
                        "label" : "Carrot counselling"
                      },
                      "count": 2,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": {
                        "id" : "Emu entrepreneurship",
                        "label" : "Emu entrepreneurship"
                      },
                      "count": 1,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": {
                        "id" : "Falcon finances",
                        "label" : "Falcon finances"
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

  it("aggregates by the subject") {
    val images = Seq(
      "images.subjects.screwdrivers-1",
      "images.subjects.screwdrivers-2",
      "images.subjects.sounds",
      "images.subjects.squirrel,screwdriver",
      "images.subjects.squirrel,sample"
    )
    val displayImages = images
      .map(getDisplayImage)
      .sortBy(w => getKey(w, "id").get.asString)
      .map(_.withIncludes(SingleImageIncludes.none).noSpaces)

    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestDocuments(imagesIndex, images: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/images?aggregations=source.subjects.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = images.size)},
              "aggregations": {
                "type" : "Aggregations",
                "source.subjects.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 3,
                      "data" : {
                        "id" : "Simple screwdrivers",
                        "label" : "Simple screwdrivers"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : {
                        "id" : "Squashed squirrels",
                        "label" : "Squashed squirrels"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "Square sounds",
                        "label" : "Square sounds"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "label" : "Struck samples",
                        "id" : "Struck samples"
                      },
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

  // aggregate by subjects
}
