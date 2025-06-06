package weco.api.search.images

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.models.request.SingleImageIncludes

class ImagesFilteredAggregationsTest extends AnyFunSpec with ApiImagesTestBase {
  it("filters and aggregates by license") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestDocuments(
          imagesIndex,
          (0 to 6).map(i => s"images.different-licenses.$i"): _*
        )

        val ccByImages = (0 to 4)
          .map(i => s"images.different-licenses.$i")
          .map { getDisplayImage }
          .map { _.withIncludes(SingleImageIncludes.none) }
          .sortBy(w => getKey(w, "id").get.asString)

        assertJsonResponse(
          routes,
          path =
            s"$rootPath/images?aggregations=locations.license&locations.license=cc-by"
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
                ${ccByImages.mkString(",")}
              ]
            }
          """
        }
    }
  }
}
