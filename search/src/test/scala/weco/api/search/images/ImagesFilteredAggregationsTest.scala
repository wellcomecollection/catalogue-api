package weco.api.search.images

import weco.api.search.models.request.SingleImageIncludes

class ImagesFilteredAggregationsTest extends ApiImagesTestBase {
  it("filters and aggregates by license") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(
          imagesIndex, (0 to 6).map(i => s"images.different-licenses.$i"): _*
        )

        val ccByImages = (0 to 4)
          .map(i => s"images.different-licenses.$i")
          .map { getDisplayImage }
          .map { _.withIncludes(SingleImageIncludes.none) }
          .sortBy(w => getKey(w, "id").get.asString)

        assertJsonResponse(
          routes,
          path = s"$rootPath/images?aggregations=locations.license&locations.license=cc-by"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = ccByImages.length)},
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
                ${ccByImages.mkString(",")}
              ]
            }
          """
        }
    }
  }
}
